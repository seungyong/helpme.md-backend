package seungyong.helpmebackend.repository.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.repository.application.dto.ReadmeContext;
import seungyong.helpmebackend.repository.application.port.in.command.CreateReadmePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.in.command.EvaluateDraftReadmeCommand;
import seungyong.helpmebackend.repository.application.port.in.command.GenerateDraftReadmeCommand;
import seungyong.helpmebackend.repository.application.port.in.result.GeneratedReadmeResult;
import seungyong.helpmebackend.repository.application.port.in.result.ReadmeEvaluationResult;
import seungyong.helpmebackend.repository.application.port.out.GPTPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryContentPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryMutationPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryQueryPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.ReadmePushCommand;
import seungyong.helpmebackend.repository.application.port.out.result.EvaluationContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryResult;
import seungyong.helpmebackend.repository.domain.entity.Repository;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.sse.domain.type.SSETaskName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {
    private static final long USER_ID = 1L;
    private static final String OWNER = "owner";
    private static final String NAME = "repo";
    private static final String TOKEN = "access-token";

    @Mock private GithubAccessTokenProvider githubAccessTokenProvider;
    @Mock private RepositoryQueryPortOut repositoryQueryPortOut;
    @Mock private RepositoryContentPortOut repositoryContentPortOut;
    @Mock private RepositoryMutationPortOut repositoryMutationPortOut;
    @Mock private ReadmeContextLoader readmeContextLoader;
    @Mock private ReadmeAsyncResultStore readmeAsyncResultStore;
    @Mock private ReadmeSectionWriter readmeSectionWriter;
    @Mock private GPTPortOut gptPortOut;

    @InjectMocks private RepositoryService repositoryService;

    @Nested
    @DisplayName("Repository 조회")
    class RepositoryQueries {
        @Test
        @DisplayName("설치의 Repository 목록을 application result로 반환한다")
        void getRepositories() {
            Repository repository = new Repository("avatar", NAME, OWNER);
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(repositoryQueryPortOut.getRepositoriesByInstallationId(USER_ID, TOKEN, 10L, 1, 30))
                    .willReturn(new RepositoryResult(List.of(repository), 1));

            var result = repositoryService.getRepositories(USER_ID, 10L, 1, 30);

            assertThat(result.repositories()).containsExactly(repository);
            assertThat(result.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Repository 상세를 web DTO 없이 반환한다")
        void getRepository() {
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(repositoryQueryPortOut.getRepository(any()))
                    .willReturn(new RepositoryDetailResult("avatar", OWNER, NAME, "main"));

            var result = repositoryService.getRepository(USER_ID, OWNER, NAME);

            assertThat(result.owner()).isEqualTo(OWNER);
            assertThat(result.defaultBranch()).isEqualTo("main");
        }

        @Test
        @DisplayName("기본 브랜치와 브랜치 목록을 조합한다")
        void getBranches() {
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(repositoryQueryPortOut.getRepository(any()))
                    .willReturn(new RepositoryDetailResult("avatar", OWNER, NAME, "main"));
            given(repositoryQueryPortOut.getAllBranches(any()))
                    .willReturn(List.of("main", "dev"));

            var result = repositoryService.getBranches(USER_ID, OWNER, NAME);

            assertThat(result.defaultBranch()).isEqualTo("main");
            assertThat(result.branches()).containsExactly("main", "dev");
        }
    }

    @Nested
    @DisplayName("SSE fallback")
    class Fallback {
        @Test
        @DisplayName("평가 fallback 저장소에 위임한다")
        void evaluation() {
            ReadmeEvaluationResult expected = new ReadmeEvaluationResult(4.5f, List.of("good"));
            given(readmeAsyncResultStore.getEvaluation("task")).willReturn(expected);

            assertThat(repositoryService.fallbackDraftEvaluation("task")).isEqualTo(expected);
        }

        @Test
        @DisplayName("생성 fallback 저장소에 위임한다")
        void generation() {
            GeneratedReadmeResult expected = new GeneratedReadmeResult(List.of());
            given(readmeAsyncResultStore.getGeneratedReadme("task")).willReturn(expected);

            assertThat(repositoryService.fallbackGenerateReadme("task")).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("README Pull Request")
    class PullRequest {
        private final CreateReadmePullRequestCommand command =
                new CreateReadmePullRequestCommand(USER_ID, OWNER, NAME, "main", "# README");

        @Test
        @DisplayName("브랜치 생성, README push, PR 생성 순서로 완료한다")
        void success() {
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(repositoryContentPortOut.getRecentSHA(any())).willReturn("latest-sha");
            given(repositoryContentPortOut.getReadmeSHA(any())).willReturn("readme-sha");
            given(repositoryMutationPortOut.createPullRequest(any()))
                    .willReturn("https://github.com/pull/1");

            var result = repositoryService.createPullRequest(command);

            assertThat(result.htmlUrl()).isEqualTo("https://github.com/pull/1");
            verify(repositoryMutationPortOut).createBranch(any());
            verify(repositoryMutationPortOut).push(any(ReadmePushCommand.class));
            verify(repositoryMutationPortOut).createPullRequest(any());
        }

        @Test
        @DisplayName("README push 실패 시 생성한 브랜치를 정리한다")
        void pushFailure() {
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(repositoryContentPortOut.getRecentSHA(any())).willReturn("latest-sha");
            given(repositoryContentPortOut.getReadmeSHA(any())).willReturn("readme-sha");
            doThrow(new IllegalStateException("push failed"))
                    .when(repositoryMutationPortOut).push(any());

            assertThatThrownBy(() -> repositoryService.createPullRequest(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.PUSH_FAILED);

            verify(repositoryMutationPortOut).deleteBranch(any());
            verify(repositoryMutationPortOut, never()).createPullRequest(any());
        }

        @Test
        @DisplayName("PR 생성 실패 시 생성한 브랜치를 정리한다")
        void pullRequestFailure() {
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(repositoryContentPortOut.getRecentSHA(any())).willReturn("latest-sha");
            given(repositoryContentPortOut.getReadmeSHA(any())).willReturn("readme-sha");
            given(repositoryMutationPortOut.createPullRequest(any()))
                    .willThrow(new IllegalStateException("PR failed"));

            assertThatThrownBy(() -> repositoryService.createPullRequest(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            RepositoryErrorCode.PR_CREATION_FAILED
                    );

            verify(repositoryMutationPortOut).deleteBranch(any());
        }

        @Test
        @DisplayName("브랜치 정리 실패가 원래 push 오류를 가리지 않는다")
        void cleanupFailureDoesNotMaskOriginalError() {
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(repositoryContentPortOut.getRecentSHA(any())).willReturn("latest-sha");
            given(repositoryContentPortOut.getReadmeSHA(any())).willReturn("readme-sha");
            doThrow(new IllegalStateException("push failed"))
                    .when(repositoryMutationPortOut).push(any());
            doThrow(new IllegalStateException("cleanup failed"))
                    .when(repositoryMutationPortOut).deleteBranch(any());

            assertThatThrownBy(() -> repositoryService.createPullRequest(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.PUSH_FAILED);
        }
    }

    @Nested
    @DisplayName("README 비동기 작업")
    class AsyncReadme {
        @Test
        @DisplayName("평가 결과를 application result로 발행한다")
        void evaluateSuccess() {
            EvaluateDraftReadmeCommand command = new EvaluateDraftReadmeCommand(
                    USER_ID, OWNER, NAME, "main", "# README", "evaluation-task"
            );
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(readmeContextLoader.load(USER_ID, OWNER, NAME, TOKEN, "main"))
                    .willReturn(context());
            given(gptPortOut.evaluateReadme(any()))
                    .willReturn(new EvaluationContentResult(4.5f, List.of("good")));

            repositoryService.evaluateDraftReadme(command);

            verify(readmeAsyncResultStore).publishEvaluation(
                    "evaluation-task",
                    SSETaskName.COMPLETION_EVALUATE_DRAFT.getTaskName(),
                    new ReadmeEvaluationResult(4.5f, List.of("good"))
            );
        }

        @Test
        @DisplayName("평가 실패를 오류 이벤트로 발행한다")
        void evaluateFailure() {
            EvaluateDraftReadmeCommand command = new EvaluateDraftReadmeCommand(
                    USER_ID, OWNER, NAME, "main", "# README", "evaluation-task"
            );
            RuntimeException failure = new RuntimeException("failure");
            given(githubAccessTokenProvider.get(USER_ID)).willThrow(failure);

            repositoryService.evaluateDraftReadme(command);

            verify(readmeAsyncResultStore).publishError(
                    "evaluation-task",
                    SSETaskName.COMPLETION_EVALUATE_DRAFT_ERROR.getTaskName(),
                    failure
            );
        }

        @Test
        @DisplayName("생성 결과의 DB 저장이 끝난 뒤 결과를 발행한다")
        void generateSuccess() {
            GenerateDraftReadmeCommand command = new GenerateDraftReadmeCommand(
                    USER_ID, OWNER, NAME, "main", "generation-task"
            );
            GeneratedReadmeResult generated = new GeneratedReadmeResult(List.of());
            given(githubAccessTokenProvider.get(USER_ID)).willReturn(TOKEN);
            given(readmeContextLoader.load(USER_ID, OWNER, NAME, TOKEN, "main"))
                    .willReturn(context());
            given(gptPortOut.generateDraftReadme(any())).willReturn("# Generated");
            given(readmeSectionWriter.replace(USER_ID, OWNER + "/" + NAME, "# Generated"))
                    .willReturn(generated);

            repositoryService.generateDraftReadme(command);

            verify(readmeSectionWriter, times(1))
                    .replace(USER_ID, OWNER + "/" + NAME, "# Generated");
            verify(readmeAsyncResultStore).publishGeneratedReadme(
                    "generation-task",
                    SSETaskName.COMPLETION_GENERATE.getTaskName(),
                    generated
            );
        }
    }

    private ReadmeContext context() {
        return new ReadmeContext(
                "# Existing",
                List.of(),
                new GPTRepositoryInfoResult(
                        new String[]{"Java"},
                        "small",
                        new String[0],
                        new String[0]
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
