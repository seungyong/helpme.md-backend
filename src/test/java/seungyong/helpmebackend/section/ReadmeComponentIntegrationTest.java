package seungyong.helpmebackend.section;

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
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.section.application.ReadmeComponentRepositoryAccessResolver;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
class ReadmeComponentIntegrationTest {
    private static final String OWNER = "component-user";
    private static final String NAME = "readme-project";
    private static final String PATH = "/api/v1/repos/{owner}/{name}/components";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @MockitoBean private ReadmeComponentRepositoryAccessResolver repositoryAccessResolver;

    @Test
    @DisplayName("컴포넌트 생성·중간 삽입·이동·충돌·삭제를 HTTP와 DB로 통합")
    void componentLifecycle_success() throws Exception {
        User user = saveUser();
        Project project = saveProject(user.getId());
        Cookie[] cookies = cookies(user);
        given(repositoryAccessResolver.resolveWritable(user.getId(), OWNER, NAME))
                .willReturn(project);

        JsonNode first = postComponent(cookies, "프로젝트 소개", null);
        JsonNode second = postComponent(cookies, "기술 스택", null);
        JsonNode inserted = postComponent(cookies, "트러블 슈팅", 1);

        mockMvc.perform(get(PATH, OWNER, NAME).cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components[0].id").value(first.get("id").asLong()))
                .andExpect(jsonPath("$.components[1].id").value(inserted.get("id").asLong()))
                .andExpect(jsonPath("$.components[2].id").value(second.get("id").asLong()))
                .andExpect(jsonPath("$.components[2].version").value(1));

        mockMvc.perform(patch(PATH + "/{componentId}", OWNER, NAME, inserted.get("id").asLong())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"핵심 트러블 슈팅","orderIdx":0,"version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderIdx").value(0))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch(PATH + "/{componentId}", OWNER, NAME, first.get("id").asLong())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"오래된 수정\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENT_40901"));

        mockMvc.perform(delete(PATH + "/{componentId}", OWNER, NAME, second.get("id").asLong())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PATH, OWNER, NAME).cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.length()").value(2))
                .andExpect(jsonPath("$.components[0].title").value("핵심 트러블 슈팅"))
                .andExpect(jsonPath("$.components[0].orderIdx").value(0))
                .andExpect(jsonPath("$.components[1].orderIdx").value(1));
    }

    private JsonNode postComponent(Cookie[] cookies, String title, Integer orderIdx)
            throws Exception {
        String orderField = orderIdx == null ? "" : ",\"orderIdx\":" + orderIdx;
        String response = mockMvc.perform(post(PATH, OWNER, NAME)
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"" + orderField + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private User saveUser() {
        return userPortOut.save(new User(
                null,
                new GithubUser(
                        OWNER,
                        818181L,
                        new EncryptedToken(cipherPortOut.encrypt("raw-github-token"))
                )
        ));
    }

    private Project saveProject(Long userId) {
        return projectPortOut.save(Project.builder()
                .userId(userId)
                .repoFullName(OWNER + "/" + NAME)
                .githubRepoId(919191L)
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .build());
    }

    private Cookie[] cookies(User user) {
        JWT jwt = jwtProvider.generate(new JWTUser(
                user.getId(), user.getGithubUser().getName()
        ));
        return new Cookie[] {
                new Cookie("accessToken", jwt.getAccessToken()),
                new Cookie("refreshToken", jwt.getRefreshToken())
        };
    }
}
