package seungyong.helpmebackend.github.application.port.in;

import seungyong.helpmebackend.github.domain.entity.GithubRepository;

import java.util.Set;

public interface GithubRepositoryAccessPortIn {
    GithubRepository getRepository(
            Long userId,
            Long installationId,
            Long githubRepositoryId
    );

    void validateRepositoryBranches(
            Long userId,
            Long installationId,
            Long githubRepositoryId,
            String repositoryFullName,
            Set<String> requiredBranches
    );
}
