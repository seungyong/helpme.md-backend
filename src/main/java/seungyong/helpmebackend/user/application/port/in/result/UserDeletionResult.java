package seungyong.helpmebackend.user.application.port.in.result;

import java.time.OffsetDateTime;

public record UserDeletionResult(
        String status,
        OffsetDateTime requestedAt,
        String requiredAction
) {
}
