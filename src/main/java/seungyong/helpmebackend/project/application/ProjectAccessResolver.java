package seungyong.helpmebackend.project.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;

@Component
@RequiredArgsConstructor
public class ProjectAccessResolver {
    private final ProjectPortOut projectPortOut;

    public Project resolveActive(Long userId, Long projectId) {
        Project project = projectPortOut.getById(projectId)
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));

        if (!project.isOwnedBy(userId)) {
            throw new CustomException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
        if (!project.isActive()) {
            throw new CustomException(ProjectErrorCode.PROJECT_NOT_ACTIVE);
        }
        return project;
    }
}
