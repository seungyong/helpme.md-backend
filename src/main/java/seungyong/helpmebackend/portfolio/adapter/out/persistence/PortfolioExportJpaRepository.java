package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.portfolio.adapter.out.persistence.entity.PortfolioExportJpaEntity;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PortfolioExportJpaRepository extends JpaRepository<PortfolioExportJpaEntity, Long> {
    Optional<PortfolioExportJpaEntity> findByIdempotencyKey(UUID idempotencyKey);

    @Query("select e from PortfolioExport e where e.portfolio.id = :portfolioId and e.id = :exportId")
    Optional<PortfolioExportJpaEntity> findByPortfolioIdAndId(
            @Param("portfolioId") Long portfolioId, @Param("exportId") Long exportId
    );

    @Query("""
            select e from PortfolioExport e
            where e.portfolio.id = :portfolioId
              and (:format is null or e.format = :format)
              and (:status is null or e.status = :status)
              and (:cursorCreatedAt is null or e.createdAt < :cursorCreatedAt
                   or (e.createdAt = :cursorCreatedAt and e.id < :cursorId))
            order by e.createdAt desc, e.id desc
            """)
    List<PortfolioExportJpaEntity> findPage(
            @Param("portfolioId") Long portfolioId,
            @Param("format") PortfolioExportFormat format,
            @Param("status") PortfolioExportStatus status,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from PortfolioExport e
            where e.status = :status
               or (e.status = :processingStatus and e.startedAt is null)
            order by e.createdAt asc, e.id asc
            """)
    List<PortfolioExportJpaEntity> findClaimable(
            @Param("status") PortfolioExportStatus status,
            @Param("processingStatus") PortfolioExportStatus processingStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from PortfolioExport e
            where e.status = :status and e.startedAt < :stuckBefore
            order by e.startedAt asc
            """)
    List<PortfolioExportJpaEntity> findStuck(
            @Param("status") PortfolioExportStatus status,
            @Param("stuckBefore") OffsetDateTime stuckBefore
    );

    @Query("""
            select e from PortfolioExport e
            where e.format = :format and e.status = :status
              and e.expiresAt <= :now and e.storagePath is not null
            order by e.expiresAt asc
            """)
    List<PortfolioExportJpaEntity> findExpired(
            @Param("format") PortfolioExportFormat format,
            @Param("status") PortfolioExportStatus status,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );
}
