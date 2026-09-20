package seungyong.helpmebackend.user.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import seungyong.helpmebackend.global.application.deletion.DeletionRetryPolicy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDeletionAdapter implements UserDeletionPortOut {
    private static final List<UserStatus> DELETION_STATUSES = List.of(
            UserStatus.DELETING, UserStatus.DELETE_FAILED
    );

    private final UserJpaRepository repository;

    @Override
    @Transactional
    public User requestDeletion(Long userId, OffsetDateTime requestedAt) {
        UserJpaEntity entity = repository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        if (entity.getDeletionRequestedAt() != null) {
            return UserPersistenceMapper.INSTANCE.toDomainEntity(entity);
        }
        entity.requestDeletion(requestedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        return UserPersistenceMapper.INSTANCE.toDomainEntity(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public Optional<User> claimNext(
            OffsetDateTime now,
            OffsetDateTime deletingBefore,
            OffsetDateTime retryBefore
    ) {
        List<UserJpaEntity> candidates = repository.findDeletionCandidates(
                UserStatus.DELETING,
                UserStatus.DELETE_FAILED,
                deletingBefore,
                retryBefore, now,
                PageRequest.of(0, 1)
        );
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        UserJpaEntity candidate = candidates.get(0);
        int claimed = repository.claimDeletion(
                candidate.getId(), candidate.getUpdatedAt(), now, DELETION_STATUSES
        );
        if (claimed == 0) {
            return Optional.empty();
        }
        return repository.findById(candidate.getId())
                .map(UserPersistenceMapper.INSTANCE::toDomainEntity);
    }

    @Override
    @Transactional
    public void markFailed(Long userId, OffsetDateTime claimedAt, String errorCode, String errorMessage) {
        repository.findByIdForUpdate(userId).filter(entity -> ownsClaim(entity, claimedAt)).ifPresent(entity -> {
            entity.markDeletionFailed(errorCode, errorMessage);
            entity.recordDeletionRetry(OffsetDateTime.now(java.time.ZoneOffset.UTC));
            if (entity.getDeletionAttempts() >= DeletionRetryPolicy.FAST_ATTEMPTS) {
                log.error("Deletion fast retries exhausted; daily retry scheduled: userId={}", userId);
            }
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockClaim(Long userId, OffsetDateTime claimedAt) {
        return repository.findByIdForUpdate(userId)
                .filter(entity -> ownsClaim(entity, claimedAt)).isPresent();
    }

    private boolean ownsClaim(UserJpaEntity entity, OffsetDateTime claimedAt) {
        return claimedAt != null && DELETION_STATUSES.contains(entity.getStatus())
                && entity.getUpdatedAt().isEqual(claimedAt);
    }

    @Override
    @Transactional
    public void hardDelete(Long userId) {
        repository.findById(userId).ifPresent(repository::delete);
        repository.flush();
    }
}
