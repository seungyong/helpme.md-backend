package seungyong.helpmebackend.project.application.port.out;

import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ProjectDeletionPortOut {
    Project requestDeletion(Long projectId, OffsetDateTime requestedAt);

    void requestAllByUserId(Long userId, OffsetDateTime requestedAt);

    Optional<Project> claimNext(OffsetDateTime now, OffsetDateTime deletingBefore,
                                OffsetDateTime retryBefore);

    boolean lockClaim(Long projectId, OffsetDateTime claimedAt);

    void markFailed(Long projectId, OffsetDateTime claimedAt, String errorCode, String errorMessage);

    void hardDelete(Long projectId);
}
