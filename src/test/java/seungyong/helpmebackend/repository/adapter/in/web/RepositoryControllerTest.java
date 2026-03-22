package seungyong.helpmebackend.repository.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestDraftEvaluation;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestGeneration;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestPull;
import seungyong.helpmebackend.repository.application.port.in.RepositoryPortIn;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = RepoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
public class RepositoryControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private RepositoryPortIn repositoryPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .plugin(new JakartaValidationPlugin())
            .build();

    private final CustomUserDetails userDetails = new CustomUserDetails(1L, "test-user");

    @Nested
    @DisplayName("getRepositories - 레포지토리 목록 조회")
    class GetRepositories {
        @Test
        @DisplayName("성공")
        void getRepositories_success() throws Exception {
            mockMvc.perform(get("/api/v1/repos")
                            .param("installation_id", "123")
                            .param("page", "1")
                            .param("per_page", "30")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).getRepositories(eq(1L), eq(123L), eq(1), eq(30));
        }
    }

    @Nested
    @DisplayName("getRepository - 레포지토리 상세 조회")
    class GetRepository {
        @Test
        @DisplayName("성공")
        void getRepository_success() throws Exception {
            mockMvc.perform(get("/api/v1/repos/{owner}/{name}", "owner", "repo")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).getRepository(eq(1L), eq("owner"), eq("repo"));
        }
    }

    @Nested
    @DisplayName("getBranches - 레포지토리 브랜치 목록 조회")
    class GetBranches {
        @Test
        @DisplayName("성공")
        void getBranches_success() throws Exception {
            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/branches", "owner", "repo")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).getBranches(eq(1L), eq("owner"), eq("repo"));
        }
    }

    @Nested
    @DisplayName("getFallbackDraftEvaluation - 임시 저장된 README 초안 평가 결과 조회")
    class GetFallbackDraftEvaluation {
        @Test
        @DisplayName("성공")
        void getFallbackDraftEvaluation_success() throws Exception {
            mockMvc.perform(get("/api/v1/repos/fallback/evaluate/draft/{taskId}", "task-123")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).fallbackDraftEvaluation(eq("task-123"));
        }
    }

    @Nested
    @DisplayName("getFallbackGenerate - 임시 저장된 README 내용 조회")
    class GetFallbackGenerate {
        @Test
        @DisplayName("성공")
        void getFallbackGenerate_success() throws Exception {
            mockMvc.perform(get("/api/v1/repos/fallback/generate/{taskId}", "task-123")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).fallbackGenerateReadme(eq("task-123"));
        }
    }

    @Nested
    @DisplayName("createPullRequest - 풀 리퀘스트 생성")
    class CreatePullRequest {
        @Test
        @DisplayName("성공")
        void createPullRequest_success() throws Exception {
            RequestPull request = fixtureMonkey.giveMeOne(RequestPull.class);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}", "owner", "repo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).createPullRequest(any(RequestPull.class), eq(1L), eq("owner"), eq("repo"));
        }
    }

    @Nested
    @DisplayName("evaluateDraftReadme - README 평가")
    class EvaluateDraftReadme {
        @Test
        @DisplayName("성공")
        void evaluateDraftReadme_success() throws Exception {
            RequestDraftEvaluation request = fixtureMonkey.giveMeOne(RequestDraftEvaluation.class);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/evaluate/draft/sse", "owner", "repo")
                            .param("taskId", "task-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isAccepted());

            verify(repositoryPortIn).evaluateDraftReadme(any(RequestDraftEvaluation.class), eq("task-123"), eq(1L), eq("owner"), eq("repo"));
        }
    }

    @Nested
    @DisplayName("generateDraftReadme - README 초안 생성")
    class GenerateDraftReadme {
        @Test
        @DisplayName("성공")
        void generateDraftReadme_success() throws Exception {
            RequestGeneration request = fixtureMonkey.giveMeOne(RequestGeneration.class);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/generate/sse", "owner", "repo")
                            .param("taskId", "task-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isAccepted());

            verify(repositoryPortIn).generateDraftReadme(any(RequestGeneration.class), eq("task-123"), eq(1L), eq("owner"), eq("repo"));
        }
    }
}