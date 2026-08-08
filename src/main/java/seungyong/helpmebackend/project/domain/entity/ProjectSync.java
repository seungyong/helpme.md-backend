package seungyong.helpmebackend.project.domain.entity;

import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;

import java.time.OffsetDateTime;

public record ProjectSync(
        ProjectSyncStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        ProjectOperationError error
) {
    public ProjectSync {
        status = status == null ? ProjectSyncStatus.PENDING : status;
    }

    public static ProjectSync pending() {
        return new ProjectSync(ProjectSyncStatus.PENDING, null, null, null);
    }
}
