package seungyong.helpmebackend.user.application.port.out;

import seungyong.helpmebackend.user.domain.entity.User;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserDeletionPortOut {
    User requestDeletion(Long userId, OffsetDateTime requestedAt);

    Optional<User> claimNext(OffsetDateTime now, OffsetDateTime deletingBefore,
                             OffsetDateTime retryBefore);

    boolean lockClaim(Long userId, OffsetDateTime claimedAt);

    void markFailed(Long userId, OffsetDateTime claimedAt, String errorCode, String errorMessage);

    void hardDelete(Long userId);
}
