package seungyong.helpmebackend.user.domain.entity;

import java.time.OffsetDateTime;

public record UserDeletion(
        OffsetDateTime requestedAt,
        String errorCode,
        String errorMessage
) {
    public static UserDeletion none() {
        return new UserDeletion(null, null, null);
    }

    public boolean isRequested() {
        return requestedAt != null;
    }
}
