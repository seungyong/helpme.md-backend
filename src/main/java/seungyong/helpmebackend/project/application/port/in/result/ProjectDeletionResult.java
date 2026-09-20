package seungyong.helpmebackend.project.application.port.in.result;

import java.time.OffsetDateTime;

public record ProjectDeletionResult(
        Long projectId,
        String status,
        OffsetDateTime requestedAt
) {
}
