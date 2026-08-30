package seungyong.helpmebackend.activity.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.activity.adapter.out.persistence.entity.ActivityJpaEntity;
import seungyong.helpmebackend.activity.domain.type.ActivityType;

import java.time.OffsetDateTime;
import java.util.List;

interface ActivityJpaRepository extends JpaRepository<ActivityJpaEntity, Long> {
    boolean existsByProject_IdAndExternalKey(Long projectId, String externalKey);

    List<ActivityJpaEntity> findAllByProject_IdAndExternalKeyIn(
            Long projectId, List<String> externalKeys
    );

    @Query("""
            select a from Activity a
            where a.project.id = :projectId
              and a.occurredAt >= :from and a.occurredAt < :to
              and (:query is null or lower(a.title) like lower(concat('%', :query, '%'))
                   or lower(a.commitSha) like lower(concat('%', :query, '%')))
              and (:branch is null or a.branchName = :branch)
              and (:type is null or a.activityType = :type)
              and (:cursorOccurredAt is null or a.occurredAt < :cursorOccurredAt
                   or (a.occurredAt = :cursorOccurredAt and a.id < :cursorId))
            order by a.occurredAt desc, a.id desc
            """)
    List<ActivityJpaEntity> findPage(
            @Param("projectId") Long projectId,
            @Param("query") String query,
            @Param("branch") String branch,
            @Param("type") ActivityType type,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("cursorOccurredAt") OffsetDateTime cursorOccurredAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select count(distinct case when a.activityType = :pushType then a.webhookDelivery.id else null end),
                   sum(case when a.activityType = :pushType then 1 else 0 end),
                   coalesce(sum(a.filesChanged), 0),
                   count(distinct a.actorLogin)
            from Activity a
            where a.project.id = :projectId
              and a.occurredAt >= :from and a.occurredAt < :to
              and (:query is null or lower(a.title) like lower(concat('%', :query, '%'))
                   or lower(a.commitSha) like lower(concat('%', :query, '%')))
              and (:branch is null or a.branchName = :branch)
              and (:type is null or a.activityType = :type)
            """)
    Object[] summarize(
            @Param("projectId") Long projectId,
            @Param("query") String query,
            @Param("branch") String branch,
            @Param("type") ActivityType type,
            @Param("pushType") ActivityType pushType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
            select a from Activity a
            where a.project.id = :projectId
              and a.occurredAt >= :from and a.occurredAt < :to
            order by a.occurredAt desc, a.id desc
            """)
    List<ActivityJpaEntity> findEvidence(
            @Param("projectId") Long projectId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

    long countByProject_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            Long projectId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
