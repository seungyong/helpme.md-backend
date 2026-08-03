package seungyong.helpmebackend.github.application.port.in;

import seungyong.helpmebackend.github.application.port.in.result.GithubInstallationsResult;
import seungyong.helpmebackend.github.application.port.in.result.GithubRepositoriesResult;

public interface GithubAppPortIn {
    GithubInstallationsResult getInstallations(Long userId);

    GithubRepositoriesResult getRepositories(
            Long userId,
            Long installationId,
            String query,
            String cursor,
            Integer size
    );
}
