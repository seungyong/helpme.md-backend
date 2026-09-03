package seungyong.helpmebackend.project.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectActivityOverviewProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectBranchCountProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectCountProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectDailyReflectionProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectLatestActivityProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectRecentActivityProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectReflectionOverviewProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectWeekDailyReflectionProjection;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

interface ProjectQueryJpaRepository extends Repository<ProjectJpaEntity, Long> {
    @Query("""
            select p.id from Project p
            where p.user.id = :userId
            order by p.createdAt asc, p.id asc
            """)
    List<Long> findUnlockedProjectIds(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            select p from Project p
            where p.user.id = :userId
              and p.status = :status
              and (:cursorCreatedAt is null or p.createdAt < :cursorCreatedAt
                   or (p.createdAt = :cursorCreatedAt and p.id < :cursorId))
            order by p.createdAt desc, p.id desc
            """)
    List<ProjectJpaEntity> findActivePage(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select p from Project p
            where p.user.id = :userId
              and p.status = :status
              and (:cursorCreatedAt is null or p.createdAt < :cursorCreatedAt
                   or (p.createdAt = :cursorCreatedAt and p.id < :cursorId))
              and (p.syncStatus = :failed
                   or p.webhookStatus in :warningWebhookStatuses
                   or p.id not in :unlockedIds)
            order by p.createdAt desc, p.id desc
            """)
    List<ProjectJpaEntity> findAttentionRequiredPage(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status,
            @Param("failed") ProjectSyncStatus failed,
            @Param("warningWebhookStatuses") Collection<ProjectWebhookStatus> warningWebhookStatuses,
            @Param("unlockedIds") Collection<Long> unlockedIds,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select a.project.id as projectId, count(a) as count
            from Activity a
            where a.project.id in :projectIds and a.occurredAt >= :metricFrom
            group by a.project.id
            """)
    List<ProjectCountProjection> countRecentActivitiesByProjectIds(
            @Param("projectIds") Collection<Long> projectIds,
            @Param("metricFrom") OffsetDateTime metricFrom
    );

    @Query("""
            select r.project.id as projectId, count(r) as count
            from Reflection r
            where r.project.id in :projectIds and r.status = :status
            group by r.project.id
            """)
    List<ProjectCountProjection> countReflectionsByProjectIds(
            @Param("projectIds") Collection<Long> projectIds,
            @Param("status") ReflectionStatus status
    );

    @Query("""
            select a.project.id as projectId, a.title as title
            from Activity a
            where a.project.id in :projectIds
              and not exists (
                select newer.id from Activity newer
                where newer.project.id = a.project.id
                  and (newer.occurredAt > a.occurredAt
                       or (newer.occurredAt = a.occurredAt and newer.id > a.id))
              )
            """)
    List<ProjectLatestActivityProjection> findLatestActivitiesByProjectIds(
            @Param("projectIds") Collection<Long> projectIds
    );

    @Query("select count(a) from Activity a where a.project.id = :projectId")
    long countAllActivities(@Param("projectId") Long projectId);

    @Query("""
            select
              coalesce(sum(case when a.occurredAt >= :currentFrom
                                    and a.occurredAt < :currentTo then 1 else 0 end), 0)
                                    as currentEventCount,
              coalesce(sum(case when a.occurredAt >= :previousFrom
                                    and a.occurredAt < :currentFrom then 1 else 0 end), 0)
                                    as previousEventCount,
              coalesce(sum(case when a.activityType = :commitType
                                    and a.occurredAt >= :currentFrom
                                    and a.occurredAt < :currentTo then 1 else 0 end), 0)
                                    as currentCommitCount,
              coalesce(sum(case when a.activityType = :commitType
                                    and a.occurredAt >= :previousFrom
                                    and a.occurredAt < :currentFrom then 1 else 0 end), 0)
                                    as previousCommitCount,
              coalesce(sum(case when a.occurredAt >= :todayFrom
                                    and a.occurredAt < :todayTo then 1 else 0 end), 0)
                                    as todayActivityCount
            from Activity a
            where a.project.id = :projectId
              and a.occurredAt >= :previousFrom
              and a.occurredAt < :currentTo
            """)
    ProjectActivityOverviewProjection summarizeActivities(
            @Param("projectId") Long projectId,
            @Param("commitType") ActivityType commitType,
            @Param("previousFrom") OffsetDateTime previousFrom,
            @Param("currentFrom") OffsetDateTime currentFrom,
            @Param("currentTo") OffsetDateTime currentTo,
            @Param("todayFrom") OffsetDateTime todayFrom,
            @Param("todayTo") OffsetDateTime todayTo
    );

    @Query("""
            select a.branchName as branchName, count(a) as count
            from Activity a
            where a.project.id = :projectId
              and a.activityType = :commitType
              and a.occurredAt >= :currentFrom
              and a.occurredAt < :currentTo
            group by a.branchName
            order by count(a) desc, a.branchName asc
            """)
    List<ProjectBranchCountProjection> countCommitsByBranch(
            @Param("projectId") Long projectId,
            @Param("commitType") ActivityType commitType,
            @Param("currentFrom") OffsetDateTime currentFrom,
            @Param("currentTo") OffsetDateTime currentTo
    );

    @Query("""
            select
              coalesce(sum(case when r.kind = :dailyKind and r.status = :saved
                                    and r.periodStart >= :currentPeriodFrom
                                    and r.periodStart <= :currentPeriodTo then 1 else 0 end), 0)
                                    as currentDailySavedCount,
              coalesce(sum(case when r.kind = :dailyKind and r.status = :saved
                                    and r.periodStart >= :previousPeriodFrom
                                    and r.periodStart < :currentPeriodFrom then 1 else 0 end), 0)
                                    as previousDailySavedCount,
              coalesce(sum(case when r.kind = :weeklyKind and r.status in :completed
                                    and r.periodStart >= :currentPeriodFrom
                                    and r.periodStart <= :currentPeriodTo then 1 else 0 end), 0)
                                    as currentWeeklyCount,
              coalesce(sum(case when r.kind = :weeklyKind and r.status in :completed
                                    and r.periodStart >= :previousPeriodFrom
                                    and r.periodStart < :currentPeriodFrom then 1 else 0 end), 0)
                                    as previousWeeklyCount
            from Reflection r
            where r.project.id = :projectId
              and r.periodStart >= :previousPeriodFrom
              and r.periodStart <= :currentPeriodTo
            """)
    ProjectReflectionOverviewProjection summarizeReflections(
            @Param("projectId") Long projectId,
            @Param("dailyKind") ReflectionKind dailyKind,
            @Param("weeklyKind") ReflectionKind weeklyKind,
            @Param("saved") ReflectionStatus saved,
            @Param("completed") Collection<ReflectionStatus> completed,
            @Param("previousPeriodFrom") LocalDate previousPeriodFrom,
            @Param("currentPeriodFrom") LocalDate currentPeriodFrom,
            @Param("currentPeriodTo") LocalDate currentPeriodTo
    );

    @Query("""
            select count(d) from Devlog d
            where d.project.id = :projectId and d.logDate = :today
            """)
    long countDevlogs(
            @Param("projectId") Long projectId,
            @Param("today") LocalDate today
    );

    @Query("""
            select r.id as id, r.status as status from Reflection r
            where r.project.id = :projectId
              and r.kind = :kind
              and r.periodStart = :today
            order by r.id desc
            """)
    List<ProjectDailyReflectionProjection> findDailyReflections(
            @Param("projectId") Long projectId,
            @Param("kind") ReflectionKind kind,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("""
            select a.id as id, a.activityType as activityType, a.title as title,
                   a.summary as summary, a.branchName as branchName,
                   a.commitSha as commitSha, a.filesChanged as filesChanged,
                   a.occurredAt as occurredAt
            from Activity a
            where a.project.id = :projectId
            order by a.occurredAt desc, a.id desc
            """)
    List<ProjectRecentActivityProjection> findRecentActivities(
            @Param("projectId") Long projectId,
            Pageable pageable
    );

    @Query("""
            select r.periodStart as periodStart, r.status as status, r.id as id
            from Reflection r
            where r.project.id = :projectId
              and r.kind = :kind
              and r.periodStart between :weekStart and :weekEnd
            order by r.periodStart asc, r.id asc
            """)
    List<ProjectWeekDailyReflectionProjection> findWeekDailyReflections(
            @Param("projectId") Long projectId,
            @Param("kind") ReflectionKind kind,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd
    );
}
