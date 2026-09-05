package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.reflection.adapter.out.persistence.entity.ReflectionJpaEntity;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

import java.time.LocalDate;
import java.util.List;

interface PortfolioReflectionSourceJpaRepository extends Repository<ReflectionJpaEntity, Long> {
    @Query("""
            select r from Reflection r
            where r.project.id = :projectId
              and r.status = :status
              and r.periodStart >= :periodStart
              and r.periodEnd <= :periodEnd
            order by r.periodStart desc, r.id desc
            """)
    List<ReflectionJpaEntity> findCandidates(
            @Param("projectId") Long projectId,
            @Param("status") ReflectionStatus status,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @Query("""
            select r from Reflection r
            where r.project.id = :projectId and r.status = :status and r.id in :ids
            order by r.periodStart asc, r.id asc
            """)
    List<ReflectionJpaEntity> findSelected(
            @Param("projectId") Long projectId,
            @Param("status") ReflectionStatus status,
            @Param("ids") List<Long> ids
    );

    long countByProject_IdAndStatus(Long projectId, ReflectionStatus status);
}
