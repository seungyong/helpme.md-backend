package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PortfolioJpaRepository extends JpaRepository<PortfolioJpaEntity, Long> {
    Optional<PortfolioJpaEntity> findByProject_IdAndId(Long projectId, Long id);

    Optional<PortfolioJpaEntity> findByProject_IdAndRequestKey(Long projectId, UUID requestKey);

    @Query("""
            select p from Portfolio p
            where p.project.id = :projectId
              and (:status is null or p.status = :status)
              and (:cursorUpdatedAt is null or p.updatedAt < :cursorUpdatedAt
                   or (p.updatedAt = :cursorUpdatedAt and p.id < :cursorId))
            order by p.updatedAt desc, p.id desc
            """)
    List<PortfolioJpaEntity> findPage(
            @Param("projectId") Long projectId,
            @Param("status") PortfolioStatus status,
            @Param("cursorUpdatedAt") OffsetDateTime cursorUpdatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.project.id = :projectId and p.id = :id")
    Optional<PortfolioJpaEntity> findForUpdate(@Param("projectId") Long projectId, @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Portfolio p
            where p.status = :queued
            order by p.createdAt asc, p.id asc
            """)
    List<PortfolioJpaEntity> findClaimable(@Param("queued") PortfolioStatus queued, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Portfolio p
            where p.status = :generating and p.generationStartedAt < :stuckBefore
            order by p.generationStartedAt asc, p.id asc
            """)
    List<PortfolioJpaEntity> findStuck(
            @Param("generating") PortfolioStatus generating,
            @Param("stuckBefore") OffsetDateTime stuckBefore
    );
}
