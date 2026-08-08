package seungyong.helpmebackend.project.domain.entity;

import java.time.OffsetDateTime;

public record ProjectDeletion(
        OffsetDateTime requestedAt,
        ProjectOperationError error
) {
    public static ProjectDeletion none() {
        return new ProjectDeletion(null, null);
    }
}
