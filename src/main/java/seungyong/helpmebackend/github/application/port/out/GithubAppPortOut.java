package seungyong.helpmebackend.github.application.port.out;

import seungyong.helpmebackend.github.domain.entity.GithubInstallation;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.entity.GithubRepositoryPage;

import java.util.List;
import java.util.Set;

public interface GithubAppPortOut {
    List<GithubInstallation> getInstallations(Long userId, String accessToken);

    GithubRepositoryPage getRepositories(
            Long userId,
            String accessToken,
            Long installationId,
            String query,
            int page,
            int size
    );

    GithubRepository getRepository(
            Long userId,
            String accessToken,
            Long installationId,
            Long githubRepositoryId
    );

    void validateRepositoryBranches(
            Long userId,
            String accessToken,
            Long installationId,
            Long githubRepositoryId,
            String repositoryFullName,
            Set<String> requiredBranches
    );
}
