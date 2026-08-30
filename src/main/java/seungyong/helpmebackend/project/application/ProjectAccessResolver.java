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

    /**
     * 프로젝트 접근 권한을 확인하고, 프로젝트를 반환합니다.
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID
     * @return 프로젝트 엔티티
     * @throws CustomException 프로젝트가 존재하지 않거나, 접근 권한이 없거나, 비활성화된 경우 발생
     */
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
