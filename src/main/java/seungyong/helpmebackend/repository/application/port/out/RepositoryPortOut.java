package seungyong.helpmebackend.repository.application.port.out;

import seungyong.helpmebackend.repository.application.port.out.command.RepoPermissionCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.command.*;
import seungyong.helpmebackend.repository.application.port.out.result.*;

import java.util.List;

public interface RepositoryPortOut {
    RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page, Integer perPage);
    RepositoryDetailResult getRepository(RepoInfoCommand command);
    List<RepositoryLanguageResult> getRepositoryLanguages(RepoInfoCommand command);
    List<RepositoryTreeResult> getRepositoryTree(RepoBranchCommand command);

    ContributorsResult getContributors(RepoInfoCommand info);

    String getRecentSHA(RepoBranchCommand command);
    String getReadmeSHA(RepoBranchCommand command);
    String getReadmeContent(RepoBranchCommand command);

    List<String> getAllBranches(RepoInfoCommand command);
    void createBranch(CreateBranchCommand command);
    void deleteBranch(RepoBranchCommand command);

    void push(ReadmePushCommand command);

    String createPullRequest(CreatePullRequestCommand command);

    RepositoryFileContentResult getFileContent(RepoBranchCommand command, RepositoryTreeResult file);
    boolean checkPermission(RepoPermissionCommand command);
}
