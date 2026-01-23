package seungyong.helpmebackend.usecase.port.out.github.repository;

import seungyong.helpmebackend.adapter.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.adapter.out.command.ReadmePushCommand;
import seungyong.helpmebackend.adapter.out.result.*;

import java.util.List;
import java.util.Optional;

public interface RepositoryPortOut {
    RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page);
    RepositoryDetailResult getRepository(String accessToken, String owner, String name);
    String getDefaultBranch(String accessToken, String owner, String name);
    String getRecentSHA(String accessToken, String owner, String name, String branch);
    void createBranch(String accessToken, String owner, String name, String newBranchName, String sha);
    void deleteBranch(String accessToken, String owner, String name, String branch);
    String getReadmeSHA(String accessToken, String owner, String name, String branch);
    void push(ReadmePushCommand command);
    String createPullRequest(CreatePullRequestCommand command);
    List<RepositoryLanguageResult> getRepositoryLanguages(String accessToken, String owner, String name);
    String getReadmeContent(String accessToken, String owner, String name, String branch);
    List<String> getAllBranches(String accessToken, String owner, String name);
    Optional<CommitResult> getCommitsByBranch(String accessToken, String owner, String name, String branch);
    List<RepositoryTreeResult> getRepositoryTree(String accessToken, String owner, String name, String branch);
    RepositoryFileContentResult getFileContent(String accessToken, String owner, String name, RepositoryTreeResult file);
}
