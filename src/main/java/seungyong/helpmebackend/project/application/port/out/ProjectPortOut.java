package seungyong.helpmebackend.project.application.port.out;

import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ProjectPortOut {
    Project save(Project project);

    Project updateSettings(Long projectId, ProjectSettings settings);

    Optional<Project> getById(Long projectId);

    Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName);

    Set<Long> getConnectedGithubRepoIds(Long userId, Collection<Long> githubRepoIds);
}
