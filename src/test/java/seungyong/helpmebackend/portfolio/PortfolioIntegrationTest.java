package seungyong.helpmebackend.portfolio;

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
import seungyong.helpmebackend.portfolio.application.PortfolioWorker;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioGenerationPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.result.GeneratedPortfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;
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
class PortfolioIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private ReflectionPortOut reflectionPortOut;
    @Autowired private PortfolioWorker portfolioWorker;
    @MockitoBean private PortfolioGenerationPortOut generationPortOut;

    @Test
    @DisplayName("saved 회고 근거 조회부터 queue·worker·상세·편집 저장·idempotency까지 HTTP와 DB를 통합")
    void portfolioLifecycle() throws Exception {
        User user = saveUser();
        Project project = projectPortOut.save(Project.builder()
                .userId(user.getId()).repoFullName("portfolio-user/project").githubRepoId(901L)
                .githubInstallationId(9001L).defaultBranch("main").build());
        LocalDate date = LocalDate.of(2026, 7, 25);
        Reflection reflection = reflectionPortOut.createIfAbsent(Reflection.builder()
                .projectId(project.getId()).kind(ReflectionKind.DAILY).periodStart(date).periodEnd(date)
                .title("Webhook 복구 흐름").content(new ReflectionDocument(1, List.of(
                        new ReflectionDocument.Section("summary", "markdown", "요약", "복구 흐름 구현", List.of())
                ))).status(ReflectionStatus.SAVED).sourceQuality(SourceQuality.COMPLETE)
                .sourceSnapshot(ReflectionSourceSnapshot.empty()).generationAttempts((short) 0).version(2).build()
        ).reflection();
        Cookie[] cookies = cookies(user);

        mockMvc.perform(get("/api/v1/projects/{projectId}/portfolio-sources", project.getId())
                        .cookie(cookies).param("periodStart", "2026-07-01").param("periodEnd", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibility.canCreate").value(true))
                .andExpect(jsonPath("$.reflections[0].id").value(reflection.id()))
                .andExpect(jsonPath("$.reflections[0].version").value(2));

        given(generationPortOut.generate(any())).willReturn(new GeneratedPortfolio(
                new PortfolioDocument(1, List.of(new PortfolioDocument.Section(
                        "overview", "project_overview", "프로젝트 개요", "복구 가능한 Webhook 흐름을 구현했다.",
                        List.of("reflection:" + reflection.id())
                )))
        ));
        String requestKey = "9dd7c84d-0000-4000-8000-000000000001";
        String body = """
                {"title":"기록이 성장으로 이어지는 개발 도구","periodStart":"2026-07-01",
                 "periodEnd":"2026-07-31","tone":"concise","reflectionIds":[%d],
                 "activityIds":[],"customEvidenceLinks":[],"generationMode":"ai"}
                """.formatted(reflection.id());
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/portfolios", project.getId())
                        .cookie(cookies).header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        long portfolioId = created.path("portfolioId").asLong();

        portfolioWorker.runOnce();

        mockMvc.perform(get("/api/v1/projects/{projectId}/portfolios/{portfolioId}",
                        project.getId(), portfolioId).cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.sourceSummary.reflectionCount").value(1))
                .andExpect(jsonPath("$.content.sections[0].evidenceRefs[0]")
                        .value("reflection:" + reflection.id()));

        mockMvc.perform(put("/api/v1/projects/{projectId}/portfolios/{portfolioId}",
                        project.getId(), portfolioId).cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"직접 다듬은 포트폴리오","tone":"reflection","content":{
                                  "schemaVersion":1,"sections":[{"id":"overview","type":"project_overview",
                                  "title":"프로젝트 개요","contentMd":"직접 수정한 내용",
                                  "evidenceRefs":["reflection:%d"]}]},"version":1}
                                """.formatted(reflection.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.content.sections[0].contentMd").value("직접 수정한 내용"));

        mockMvc.perform(post("/api/v1/projects/{projectId}/portfolios", project.getId())
                        .cookie(cookies).header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.status").value("saved"));
    }

    private User saveUser() {
        return userPortOut.save(new User(null, new GithubUser(
                "portfolio-user", 939393L, new EncryptedToken(cipherPortOut.encrypt("raw-github-token"))
        )));
    }

    private Cookie[] cookies(User user) {
        JWT jwt = jwtProvider.generate(new JWTUser(user.getId(), user.getGithubUser().getName()));
        return new Cookie[]{new Cookie("accessToken", jwt.getAccessToken()),
                new Cookie("refreshToken", jwt.getRefreshToken())};
    }
}
