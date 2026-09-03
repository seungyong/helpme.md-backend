package seungyong.helpmebackend.project.application.port.in;

import seungyong.helpmebackend.project.domain.entity.ProjectList;
import seungyong.helpmebackend.project.domain.entity.ProjectOverview;

public interface ProjectQueryPortIn {
    ProjectList getProjects(
            Long userId,
            String cursor,
            Integer size,
            String status
    );

    ProjectOverview getOverview(Long userId, Long projectId);
}
