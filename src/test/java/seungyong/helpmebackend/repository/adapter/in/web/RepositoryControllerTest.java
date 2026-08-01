package seungyong.helpmebackend.repository.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import seungyong.helpmebackend.global.exception.GithubRateLimitException;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestDraftEvaluation;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestGeneration;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestPull;
import seungyong.helpmebackend.repository.application.port.in.RepositoryPortIn;
import seungyong.helpmebackend.repository.application.port.in.command.CreateReadmePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.in.command.EvaluateDraftReadmeCommand;
import seungyong.helpmebackend.repository.application.port.in.command.GenerateDraftReadmeCommand;
import seungyong.helpmebackend.repository.application.port.in.result.GeneratedReadmeResult;
import seungyong.helpmebackend.repository.application.port.in.result.PullRequestResult;
import seungyong.helpmebackend.repository.application.port.in.result.ReadmeEvaluationResult;
import seungyong.helpmebackend.repository.application.port.in.result.RepositoryBranchesResult;
import seungyong.helpmebackend.repository.application.port.in.result.RepositoryDetailsResult;
import seungyong.helpmebackend.repository.application.port.in.result.RepositoryListResult;
import seungyong.helpmebackend.repository.domain.entity.Repository;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static seungyong.helpmebackend.support.fixture.TestFixtures.requestDraftEvaluation;
import static seungyong.helpmebackend.support.fixture.TestFixtures.requestGeneration;
import static seungyong.helpmebackend.support.fixture.TestFixtures.requestPull;

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

    private final CustomUserDetails userDetails = new CustomUserDetails(1L, "test-user");

    @Nested
    @DisplayName("getRepositories - 레포지토리 목록 조회")
    class GetRepositories {
        @Test
        @DisplayName("성공")
        void getRepositories_success() throws Exception {
            given(repositoryPortIn.getRepositories(1L, 123L, 1, 30))
                    .willReturn(new RepositoryListResult(
                            List.of(new Repository("avatar", "repo", "owner")), 1
                    ));

            mockMvc.perform(get("/api/v1/repos")
                            .param("installation_id", "123")
                            .param("page", "1")
                            .param("per_page", "30")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).getRepositories(eq(1L), eq(123L), eq(1), eq(30));
        }

        @Test
        @DisplayName("GitHub rate limit은 429와 Retry-After 헤더로 반환한다")
        void getRepositories_rateLimited() throws Exception {
            given(repositoryPortIn.getRepositories(1L, 123L, 1, 30))
                    .willThrow(new GithubRateLimitException(17));

            mockMvc.perform(get("/api/v1/repos")
                            .param("installation_id", "123")
                            .param("page", "1")
                            .param("per_page", "30")
                            .with(user(userDetails)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "17"))
                    .andExpect(jsonPath("$.status").value(429))
                    .andExpect(jsonPath("$.errorCode").value("REPO_42901"));
        }
    }

    @Nested
    @DisplayName("getRepository - 레포지토리 상세 조회")
    class GetRepository {
        @Test
        @DisplayName("성공")
        void getRepository_success() throws Exception {
            given(repositoryPortIn.getRepository(1L, "owner", "repo"))
                    .willReturn(new RepositoryDetailsResult("owner", "repo", "avatar", "main"));

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
            given(repositoryPortIn.getBranches(1L, "owner", "repo"))
                    .willReturn(new RepositoryBranchesResult("main", List.of("main", "dev")));

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
            given(repositoryPortIn.fallbackDraftEvaluation("task-123"))
                    .willReturn(new ReadmeEvaluationResult(4.0F, List.of("좋아요")));

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
            given(repositoryPortIn.fallbackGenerateReadme("task-123"))
                    .willReturn(new GeneratedReadmeResult(List.of(
                            new GeneratedReadmeResult.Section(1L, "Overview", "content", 1)
                    )));

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
            RequestPull request = requestPull();
            given(repositoryPortIn.createPullRequest(any(CreateReadmePullRequestCommand.class)))
                    .willReturn(new PullRequestResult("https://github.com/pull/1"));

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}", "owner", "repo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(repositoryPortIn).createPullRequest(any(CreateReadmePullRequestCommand.class));
        }
    }

    @Nested
    @DisplayName("evaluateDraftReadme - README 평가")
    class EvaluateDraftReadme {
        @Test
        @DisplayName("성공")
        void evaluateDraftReadme_success() throws Exception {
            RequestDraftEvaluation request = requestDraftEvaluation();

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/evaluate/draft/sse", "owner", "repo")
                            .param("taskId", "task-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isAccepted());

            verify(repositoryPortIn).evaluateDraftReadme(any(EvaluateDraftReadmeCommand.class));
        }
    }

    @Nested
    @DisplayName("generateDraftReadme - README 초안 생성")
    class GenerateDraftReadme {
        @Test
        @DisplayName("성공")
        void generateDraftReadme_success() throws Exception {
            RequestGeneration request = requestGeneration();

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/generate/sse", "owner", "repo")
                            .param("taskId", "task-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isAccepted());

            verify(repositoryPortIn).generateDraftReadme(any(GenerateDraftReadmeCommand.class));
        }
    }
}
