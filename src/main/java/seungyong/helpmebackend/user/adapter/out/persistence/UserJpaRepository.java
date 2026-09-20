package seungyong.helpmebackend.user.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByGithubId(Long githubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<UserJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select u from User u
            where ((u.status = :deleting and u.deletionRequestedAt <= :deletingBefore
                    and u.updatedAt <= :retryBefore)
               or (u.status = :failed and u.updatedAt <= :retryBefore))
              and (u.deletionNextRetryAt is null or u.deletionNextRetryAt <= :now)
            order by u.deletionRequestedAt asc, u.id asc
            """)
    List<UserJpaEntity> findDeletionCandidates(
            @Param("deleting") UserStatus deleting,
            @Param("failed") UserStatus failed,
            @Param("deletingBefore") OffsetDateTime deletingBefore,
            @Param("retryBefore") OffsetDateTime retryBefore,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u set u.updatedAt = :claimedAt
            where u.id = :userId and u.updatedAt = :expectedUpdatedAt
              and u.status in :statuses
            """)
    int claimDeletion(
            @Param("userId") Long userId,
            @Param("expectedUpdatedAt") OffsetDateTime expectedUpdatedAt,
            @Param("claimedAt") OffsetDateTime claimedAt,
            @Param("statuses") Collection<UserStatus> statuses
    );
}
