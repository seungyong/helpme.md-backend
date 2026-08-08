package seungyong.helpmebackend.github.application.port.in;

import java.util.Set;

public interface GithubRepositoryAccessPortIn {
    void validateRepositoryBranches(
            Long userId,
            Long installationId,
            Long githubRepositoryId,
            String repositoryFullName,
            Set<String> requiredBranches
    );
}
