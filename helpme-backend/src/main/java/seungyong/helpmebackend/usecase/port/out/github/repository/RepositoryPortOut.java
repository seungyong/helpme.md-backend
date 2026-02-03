package seungyong.helpmebackend.usecase.port.out.github.repository;

import seungyong.helpmebackend.adapter.out.command.*;
import seungyong.helpmebackend.adapter.out.result.*;

import java.util.List;
import java.util.Optional;

public interface RepositoryPortOut {
    RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page, Integer perPage);
    RepositoryDetailResult getRepository(RepoInfoCommand command);
    String getRecentSHA(RepoBranchCommand command);
    void createBranch(CreateBranchCommand command);
    void deleteBranch(RepoBranchCommand command);
    String getReadmeSHA(RepoBranchCommand command);
    void push(ReadmePushCommand command);
    String createPullRequest(CreatePullRequestCommand command);
    List<RepositoryLanguageResult> getRepositoryLanguages(RepoInfoCommand command);
    String getReadmeContent(RepoBranchCommand command);
    List<String> getAllBranches(RepoInfoCommand command);
    Optional<CommitResult> getCommitsByBranch(RepoBranchCommand command);
    List<RepositoryTreeResult> getRepositoryTree(RepoBranchCommand command);
    RepositoryFileContentResult getFileContent(RepoBranchCommand command, RepositoryTreeResult file);
}
