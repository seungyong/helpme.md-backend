package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import seungyong.helpmebackend.project.domain.entity.Project;

public record ResponseSyncRetry(
        Long projectId,
        String syncStatus,
        String location,
        int retryAfterSeconds
) {
    public static ResponseSyncRetry from(Project project) {
        return new ResponseSyncRetry(
                project.getId(), project.getSync().status().getDatabaseValue(),
                "/api/v1/projects/" + project.getId(), 2
        );
    }
}
