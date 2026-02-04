package seungyong.helpmebackend.usecase.port.out.project;

import seungyong.helpmebackend.domain.entity.project.Project;

import java.util.Optional;

public interface ProjectPortOut {
    Project save(Project project);

    Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName);
}
