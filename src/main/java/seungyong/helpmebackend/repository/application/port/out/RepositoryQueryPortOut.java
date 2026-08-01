package seungyong.helpmebackend.repository.application.port.out;

import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoPermissionCommand;
import seungyong.helpmebackend.repository.application.port.out.result.ContributorsResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryResult;

import java.util.List;

public interface RepositoryQueryPortOut {
    RepositoryResult getRepositoriesByInstallationId(
            Long userId,
            String accessToken,
            Long installationId,
            Integer page,
            Integer perPage
    );

    RepositoryDetailResult getRepository(RepoInfoCommand command);
    ContributorsResult getContributors(RepoInfoCommand command);
    List<RepositoryLanguageResult> getRepositoryLanguages(RepoInfoCommand command);
    List<String> getAllBranches(RepoInfoCommand command);
    boolean checkPermission(RepoPermissionCommand command);
}
