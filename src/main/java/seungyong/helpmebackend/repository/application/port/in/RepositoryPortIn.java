package seungyong.helpmebackend.repository.application.port.in;

import seungyong.helpmebackend.repository.application.port.in.command.CreateReadmePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.in.command.EvaluateDraftReadmeCommand;
import seungyong.helpmebackend.repository.application.port.in.command.GenerateDraftReadmeCommand;
import seungyong.helpmebackend.repository.application.port.in.result.GeneratedReadmeResult;
import seungyong.helpmebackend.repository.application.port.in.result.PullRequestResult;
import seungyong.helpmebackend.repository.application.port.in.result.ReadmeEvaluationResult;
import seungyong.helpmebackend.repository.application.port.in.result.RepositoryBranchesResult;
import seungyong.helpmebackend.repository.application.port.in.result.RepositoryDetailsResult;
import seungyong.helpmebackend.repository.application.port.in.result.RepositoryListResult;

public interface RepositoryPortIn {
    RepositoryListResult getRepositories(Long userId, Long installationId, Integer page, Integer perPage);
    RepositoryDetailsResult getRepository(Long userId, String owner, String name);
    RepositoryBranchesResult getBranches(Long userId, String owner, String name);
    ReadmeEvaluationResult fallbackDraftEvaluation(String taskId);
    GeneratedReadmeResult fallbackGenerateReadme(String taskId);
    PullRequestResult createPullRequest(CreateReadmePullRequestCommand command);
    void evaluateDraftReadme(EvaluateDraftReadmeCommand command);
    void generateDraftReadme(GenerateDraftReadmeCommand command);
}
