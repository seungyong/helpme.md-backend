package seungyong.helpmebackend.project.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.util.Collection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, Long> {
    Optional<ProjectJpaEntity> findByUser_IdAndRepoFullName(Long userId, String repoFullName);

    Optional<ProjectJpaEntity> findByUser_IdAndGithubRepoId(Long userId, Long githubRepoId);

    long countByUser_Id(Long userId);

    List<ProjectJpaEntity> findAllByGithubInstallationIdAndGithubRepoIdAndStatus(
            Long githubInstallationId,
            Long githubRepoId,
            ProjectStatus status
    );

    List<ProjectJpaEntity> findAllByStatus(ProjectStatus status);

    List<ProjectJpaEntity> findAllByUser_IdAndGithubRepoIdIn(Long userId, Collection<Long> githubRepoIds);

    List<ProjectJpaEntity> findAllByUser_IdAndStatusIn(
            Long userId,
            Collection<ProjectStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id")
    Optional<ProjectJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select p from Project p
            where ((p.status = :deleting and p.deletionRequestedAt <= :deletingBefore
                    and p.updatedAt <= :retryBefore)
               or (p.status = :failed and p.updatedAt <= :retryBefore))
              and (p.deletionNextRetryAt is null or p.deletionNextRetryAt <= :now)
            order by p.deletionRequestedAt asc, p.id asc
            """)
    List<ProjectJpaEntity> findDeletionCandidates(
            @Param("deleting") ProjectStatus deleting,
            @Param("failed") ProjectStatus failed,
            @Param("deletingBefore") OffsetDateTime deletingBefore,
            @Param("retryBefore") OffsetDateTime retryBefore,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Project p set p.updatedAt = :claimedAt
            where p.id = :projectId and p.updatedAt = :expectedUpdatedAt
              and p.status in :statuses
            """)
    int claimDeletion(
            @Param("projectId") Long projectId,
            @Param("expectedUpdatedAt") OffsetDateTime expectedUpdatedAt,
            @Param("claimedAt") OffsetDateTime claimedAt,
            @Param("statuses") Collection<ProjectStatus> statuses
    );
}
