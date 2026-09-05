package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.activity.adapter.out.persistence.entity.ActivityJpaEntity;

import java.time.OffsetDateTime;
import java.util.List;

interface PortfolioActivitySourceJpaRepository extends Repository<ActivityJpaEntity, Long> {
    @Query("""
            select a from Activity a
            where a.project.id = :projectId
              and a.occurredAt >= :from and a.occurredAt < :to
            order by a.occurredAt desc, a.id desc
            """)
    List<ActivityJpaEntity> findCandidates(
            @Param("projectId") Long projectId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
            select a from Activity a
            where a.project.id = :projectId and a.id in :ids
            order by a.occurredAt asc, a.id asc
            """)
    List<ActivityJpaEntity> findSelected(
            @Param("projectId") Long projectId,
            @Param("ids") List<Long> ids
    );
}
