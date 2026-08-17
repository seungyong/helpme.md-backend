package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import seungyong.helpmebackend.project.domain.entity.Project;

public record ResponseCreatedProject(
        Long projectId,
        String status,
        String syncStatus,
        String webhookStatus,
        String location,
        int retryAfterSeconds
) {
    public static ResponseCreatedProject from(Project project) {
        return new ResponseCreatedProject(
                project.getId(), project.getStatus().getDatabaseValue(),
                project.getSync().status().getDatabaseValue(),
                project.getWebhook().status().getDatabaseValue(),
                "/api/v1/projects/" + project.getId(), 2
        );
    }
}
