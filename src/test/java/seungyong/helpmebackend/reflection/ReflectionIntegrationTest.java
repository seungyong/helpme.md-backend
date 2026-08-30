package seungyong.helpmebackend.reflection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.application.ReflectionWorker;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionGenerationPortOut;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
class ReflectionIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private DevlogPortOut devlogPortOut;
    @Autowired private ReflectionWorker reflectionWorker;
    @MockitoBean private ReflectionGenerationPortOut generationPortOut;

    @Test
    @DisplayName("개발로그 근거부터 queue·worker·상세·편집 저장·중복 생성까지 HTTP와 DB를 통합")
    void dailyLifecycle() throws Exception {
        User user = saveUser();
        Project project = projectPortOut.save(Project.builder()
                .userId(user.getId())
                .repoFullName("reflection-user/project")
                .githubRepoId(901L)
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .build());
        LocalDate date = LocalDate.of(2026, 8, 30);
        devlogPortOut.create(project.getId(), date, "회고 API와 worker를 구현함");
        Cookie[] cookies = cookies(user);
        given(generationPortOut.generate(any(), any(), any(), any(), any()))
                .willReturn(new ReflectionGenerationPortOut.GeneratedReflection(
                        "8월 30일 개발 회고",
                        new ReflectionDocument(1, List.of(
                                new ReflectionDocument.Section(
                                        "summary", "markdown", "오늘의 요약",
                                        "회고 API와 worker를 구현했다.",
                                        List.of("devlog:1")
                                )
                        ))
                ));

        String response = mockMvc.perform(post(
                        "/api/v1/projects/{projectId}/reflections", project.getId()
                ).cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind":"daily",
                                  "periodStart":"2026-08-30",
                                  "generationMode":"ai",
                                  "allowPartial":true
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        long reflectionId = created.path("reflectionId").asLong();

        reflectionWorker.runOnce();

        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/reflections/{reflectionId}",
                        project.getId(), reflectionId
                ).cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.sourceSummary.devlogCount").value(1))
                .andExpect(jsonPath("$.content.sections[0].evidenceRefs[0]")
                        .value("devlog:1"));

        mockMvc.perform(put(
                        "/api/v1/projects/{projectId}/reflections/{reflectionId}",
                        project.getId(), reflectionId
                ).cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"수정한 회고",
                                  "content":{
                                    "schemaVersion":1,
                                    "sections":[{
                                      "id":"summary",
                                      "type":"markdown",
                                      "title":"오늘의 요약",
                                      "contentMd":"직접 다듬은 회고",
                                      "evidenceRefs":["devlog:1"]
                                    }]
                                  },
                                  "version":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.content.sections[0].contentMd")
                        .value("직접 다듬은 회고"));

        mockMvc.perform(post(
                        "/api/v1/projects/{projectId}/reflections", project.getId()
                ).cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind":"daily",
                                  "periodStart":"2026-08-30",
                                  "generationMode":"ai"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reflectionId").value(reflectionId))
                .andExpect(jsonPath("$.status").value("saved"));
    }

    private User saveUser() {
        return userPortOut.save(new User(
                null,
                new GithubUser(
                        "reflection-user",
                        828282L,
                        new EncryptedToken(cipherPortOut.encrypt("raw-github-token"))
                )
        ));
    }

    private Cookie[] cookies(User user) {
        JWT jwt = jwtProvider.generate(
                new JWTUser(user.getId(), user.getGithubUser().getName())
        );
        return new Cookie[] {
                new Cookie("accessToken", jwt.getAccessToken()),
                new Cookie("refreshToken", jwt.getRefreshToken())
        };
    }
}
