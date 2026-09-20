package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import seungyong.helpmebackend.project.application.port.in.result.ProjectDeletionResult;

import java.time.OffsetDateTime;

public record ResponseProjectDeletion(
        Long projectId,
        String status,
        OffsetDateTime requestedAt
) {
    public static ResponseProjectDeletion from(ProjectDeletionResult result) {
        return new ResponseProjectDeletion(
                result.projectId(), result.status(), result.requestedAt()
        );
    }
}
