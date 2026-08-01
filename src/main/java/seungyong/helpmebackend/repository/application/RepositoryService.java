package seungyong.helpmebackend.repository.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.repository.application.dto.ReadmeContext;
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
import seungyong.helpmebackend.repository.application.port.out.GPTPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryContentPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryMutationPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryQueryPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.CreateBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.out.command.EvaluationCommand;
import seungyong.helpmebackend.repository.application.port.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.repository.application.port.out.command.ReadmePushCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.EvaluationContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryResult;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.sse.domain.type.SSETaskName;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService implements RepositoryPortIn {
    private final GithubAccessTokenProvider githubAccessTokenProvider;
    private final RepositoryQueryPortOut repositoryQueryPortOut;
    private final RepositoryContentPortOut repositoryContentPortOut;
    private final RepositoryMutationPortOut repositoryMutationPortOut;
    private final ReadmeContextLoader readmeContextLoader;
    private final ReadmeAsyncResultStore readmeAsyncResultStore;
    private final ReadmeSectionWriter readmeSectionWriter;
    private final GPTPortOut gptPortOut;

    @Override
    public RepositoryListResult getRepositories(
            Long userId,
            Long installationId,
            Integer page,
            Integer perPage
    ) {
        RepositoryResult result = repositoryQueryPortOut.getRepositoriesByInstallationId(
                userId,
                githubAccessTokenProvider.get(userId),
                installationId,
                page,
                perPage
        );
        return new RepositoryListResult(result.repositories(), result.totalCount());
    }

    @Override
    public RepositoryDetailsResult getRepository(Long userId, String owner, String name) {
        RepositoryDetailResult repository = repositoryQueryPortOut.getRepository(
                repositoryInfo(userId, owner, name)
        );
        return new RepositoryDetailsResult(
                repository.owner(),
                repository.name(),
                repository.avatarUrl(),
                repository.defaultBranch()
        );
    }

    @Override
    public RepositoryBranchesResult getBranches(Long userId, String owner, String name) {
        RepoInfoCommand repository = repositoryInfo(userId, owner, name);
        String defaultBranch = repositoryQueryPortOut.getRepository(repository).defaultBranch();
        List<String> branches = repositoryQueryPortOut.getAllBranches(repository);
        return new RepositoryBranchesResult(defaultBranch, branches);
    }

    @Override
    public ReadmeEvaluationResult fallbackDraftEvaluation(String taskId) {
        return readmeAsyncResultStore.getEvaluation(taskId);
    }

    @Override
    public GeneratedReadmeResult fallbackGenerateReadme(String taskId) {
        return readmeAsyncResultStore.getGeneratedReadme(taskId);
    }

    @Override
    public PullRequestResult createPullRequest(CreateReadmePullRequestCommand command) {
        RepoInfoCommand repository = repositoryInfo(
                command.userId(),
                command.owner(),
                command.name()
        );
        RepoBranchCommand baseBranch = new RepoBranchCommand(repository, command.branch());
        String recentSha = repositoryContentPortOut.getRecentSHA(baseBranch);
        String proposalBranch = "readme-proposals/" + UUID.randomUUID();

        repositoryMutationPortOut.createBranch(
                new CreateBranchCommand(repository, proposalBranch, recentSha)
        );

        try {
            repositoryMutationPortOut.push(new ReadmePushCommand(
                    repository,
                    proposalBranch,
                    command.content(),
                    repositoryContentPortOut.getReadmeSHA(baseBranch),
                    "Update README.md via HelpMe"
            ));
        } catch (Exception e) {
            deleteProposalBranchQuietly(repository, proposalBranch);
            throw new CustomException(RepositoryErrorCode.PUSH_FAILED);
        }

        try {
            String pullRequestUrl = repositoryMutationPortOut.createPullRequest(
                    new CreatePullRequestCommand(
                            repository,
                            proposalBranch,
                            command.branch(),
                            "[HelpMe] Improve README.md",
                            "This pull request is created automatically by HelpMe to improve the README.md file."
                    )
            );
            return new PullRequestResult(pullRequestUrl);
        } catch (Exception e) {
            deleteProposalBranchQuietly(repository, proposalBranch);
            throw new CustomException(RepositoryErrorCode.PR_CREATION_FAILED);
        }
    }

    @Async
    @Override
    public void evaluateDraftReadme(EvaluateDraftReadmeCommand command) {
        try {
            ReadmeContext context = readmeContextLoader.load(
                    command.userId(),
                    command.owner(),
                    command.name(),
                    githubAccessTokenProvider.get(command.userId()),
                    command.branch()
            );
            EvaluationContentResult evaluation = gptPortOut.evaluateReadme(
                    new EvaluationCommand(
                            command.owner() + "/" + command.name(),
                            command.content(),
                            repositoryInfo(context),
                            context.entryContents(),
                            context.importantFileContents(),
                            context.repositoryInfo().techStack(),
                            context.repositoryInfo().projectSize()
                    )
            );
            readmeAsyncResultStore.publishEvaluation(
                    command.taskId(),
                    SSETaskName.COMPLETION_EVALUATE_DRAFT.getTaskName(),
                    new ReadmeEvaluationResult(evaluation.rating(), evaluation.contents())
            );
        } catch (Exception e) {
            readmeAsyncResultStore.publishError(
                    command.taskId(),
                    SSETaskName.COMPLETION_EVALUATE_DRAFT_ERROR.getTaskName(),
                    e
            );
        }
    }

    @Async
    @Override
    public void generateDraftReadme(GenerateDraftReadmeCommand command) {
        try {
            ReadmeContext context = readmeContextLoader.load(
                    command.userId(),
                    command.owner(),
                    command.name(),
                    githubAccessTokenProvider.get(command.userId()),
                    command.branch()
            );
            String draftReadme = gptPortOut.generateDraftReadme(
                    new GenerateReadmeCommand(
                            command.owner() + "/" + command.name(),
                            context.readme(),
                            repositoryInfo(context),
                            context.entryContents(),
                            context.importantFileContents(),
                            context.repositoryInfo().techStack(),
                            context.repositoryInfo().projectSize()
                    )
            );
            GeneratedReadmeResult result = readmeSectionWriter.replace(
                    command.userId(),
                    command.owner() + "/" + command.name(),
                    draftReadme
            );
            readmeAsyncResultStore.publishGeneratedReadme(
                    command.taskId(),
                    SSETaskName.COMPLETION_GENERATE.getTaskName(),
                    result
            );
        } catch (Exception e) {
            readmeAsyncResultStore.publishError(
                    command.taskId(),
                    SSETaskName.COMPLETION_GENERATE_ERROR.getTaskName(),
                    e
            );
        }
    }

    private RepoInfoCommand repositoryInfo(Long userId, String owner, String name) {
        return new RepoInfoCommand(userId, githubAccessTokenProvider.get(userId), owner, name);
    }

    private RepositoryInfoCommand repositoryInfo(ReadmeContext context) {
        return new RepositoryInfoCommand(
                context.languages(),
                context.commits(),
                context.trees()
        );
    }

    private void deleteProposalBranchQuietly(
            RepoInfoCommand repository,
            String proposalBranch
    ) {
        try {
            repositoryMutationPortOut.deleteBranch(
                    new RepoBranchCommand(repository, proposalBranch)
            );
        } catch (Exception cleanupException) {
            log.warn(
                    "Failed to clean up README proposal branch: exceptionType={}",
                    cleanupException.getClass().getSimpleName()
            );
        }
    }
}
