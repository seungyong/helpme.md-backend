package seungyong.helpmebackend.project.application.port.out;

import seungyong.helpmebackend.project.domain.entity.Project;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;

public interface ProjectPortOut {
    Project save(Project project);

    Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName);

    Set<Long> getConnectedGithubRepoIds(Long userId, Collection<Long> githubRepoIds);
}
