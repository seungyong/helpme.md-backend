package seungyong.helpmebackend.project.application.port.out;

import seungyong.helpmebackend.project.domain.entity.Project;

import java.util.Optional;

public interface ProjectPortOut {
    Project save(Project project);

    Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName);
}
