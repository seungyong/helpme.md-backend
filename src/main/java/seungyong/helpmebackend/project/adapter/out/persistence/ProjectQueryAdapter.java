package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.adapter.out.persistence.mapper.ProjectPersistenceMapper;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectActivityOverviewProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectCountProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectLatestActivityProjection;
import seungyong.helpmebackend.project.adapter.out.persistence.projection.ProjectReflectionOverviewProjection;
import seungyong.helpmebackend.project.application.port.out.ProjectQueryPortOut;
import seungyong.helpmebackend.project.application.port.out.query.ProjectOverviewQuery;
import seungyong.helpmebackend.project.application.port.out.result.ProjectListQueryResult;
import seungyong.helpmebackend.project.application.port.out.result.ProjectOverviewQueryResult;
import seungyong.helpmebackend.project.domain.entity.ProjectList;
import seungyong.helpmebackend.project.domain.entity.ProjectOverview;
import seungyong.helpmebackend.project.domain.type.ProjectListStatus;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProjectQueryAdapter implements ProjectQueryPortOut {
    private static final List<ProjectWebhookStatus> WARNING_WEBHOOK_STATUSES = List.of(
            ProjectWebhookStatus.DEGRADED,
            ProjectWebhookStatus.DISCONNECTED
    );

    private final ProjectQueryJpaRepository projectQueryJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public ProjectListQueryResult findProjects(
            Long userId,
            int effectiveLimit,
            ProjectListStatus status,
            OffsetDateTime metricFrom,
            OffsetDateTime cursorCreatedAt,
            Long cursorId,
            int size
    ) {
        // 플랜에 의해 잠금 해제된 프로젝트 ID를 조회
        // 현재는 생성일, ID 기준으로 오래된 순서로 정렬
        // TODO : 유료 플랜이였다가 무료 플랜으로 변경된 경우, 어떤 프로젝트가 잠금 해제될지 정책을 정의해야 함 (사용자의 선택권이 필요할 수 있음)
        Set<Long> unlockedIds = new HashSet<>(
                projectQueryJpaRepository.findUnlockedProjectIds(
                        userId, PageRequest.of(0, effectiveLimit)
                )
        );

        // 커서 기준으로 size + 1개의 프로젝트를 조회하여 다음 페이지 존재 여부를 확인
        List<ProjectJpaEntity> entities = findProjectPage(
                userId, status, unlockedIds, cursorCreatedAt, cursorId, size
        );

        // 페이지네이션 처리: 조회된 엔티티 수가 요청한 size보다 많으면 다음 페이지가 존재함
        boolean hasNext = entities.size() > size;
        List<ProjectJpaEntity> pageEntities = hasNext
                ? new ArrayList<>(entities.subList(0, size)) : entities;
        List<Long> projectIds = pageEntities.stream().map(ProjectJpaEntity::getId).toList();

        // 각 프로젝트별 최근 활동 수, 회고 수, 최신 활동 제목을 조회
        Map<Long, Long> activityCounts = projectIds.isEmpty()
                ? Map.of() : countMap(projectQueryJpaRepository
                .countRecentActivitiesByProjectIds(projectIds, metricFrom));
        Map<Long, Long> reflectionCounts = projectIds.isEmpty()
                ? Map.of() : countMap(projectQueryJpaRepository
                .countReflectionsByProjectIds(projectIds, ReflectionStatus.SAVED));
        Map<Long, String> latestActivityTitles = projectIds.isEmpty()
                ? Map.of() : projectQueryJpaRepository
                .findLatestActivitiesByProjectIds(projectIds).stream()
                .collect(Collectors.toMap(
                        ProjectLatestActivityProjection::getProjectId,
                        ProjectLatestActivityProjection::getTitle,
                        (first, ignored) -> first
                ));

        List<ProjectList.Item> items = pageEntities.stream().map(entity -> {
            // 플랜에 의해 잠금된 프로젝트인지 여부를 판단
            boolean locked = !unlockedIds.contains(entity.getId());

            // 동기화 실패, 경고 상태인 경우 주의가 필요한 프로젝트로 판단
            boolean attentionRequired = locked
                    || entity.getSyncStatus() == ProjectSyncStatus.FAILED
                    || WARNING_WEBHOOK_STATUSES.contains(entity.getWebhookStatus());

            return new ProjectList.Item(
                    ProjectPersistenceMapper.INSTANCE.toDomainEntity(entity),
                    locked,
                    attentionRequired,
                    new ProjectList.Metrics(
                            activityCounts.getOrDefault(entity.getId(), 0L),
                            reflectionCounts.getOrDefault(entity.getId(), 0L),
                            latestActivityTitles.get(entity.getId())
                    )
            );
        }).toList();

        // 다음 페이지가 존재하면 마지막 엔티티를 기준으로 커서를 생성
        String nextCursor = hasNext && !pageEntities.isEmpty()
                ? encodeCursor(pageEntities.get(pageEntities.size() - 1)) : null;
        return new ProjectListQueryResult(items, nextCursor, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectOverviewQueryResult findOverview(
            Long projectId,
            ProjectOverviewQuery query,
            int recentActivityLimit
    ) {
        // 최근 7일과 직전 7일의 활동 집계 조회
        ProjectActivityOverviewProjection activities =
                projectQueryJpaRepository.summarizeActivities(
                        projectId,
                        ActivityType.PUSH_COMMIT,
                        query.previousFrom(),
                        query.currentFrom(),
                        query.currentTo(),
                        query.todayFrom(),
                        query.todayTo()
                );

        // 일일 회고는 SAVED 상태만, 주간 회고는 DRAFT와 SAVED 상태를 모두 포함하여 집계 조회
        ProjectReflectionOverviewProjection reflections =
                projectQueryJpaRepository.summarizeReflections(
                        projectId,
                        ReflectionKind.DAILY,
                        ReflectionKind.WEEKLY,
                        ReflectionStatus.SAVED,
                        List.of(ReflectionStatus.DRAFT, ReflectionStatus.SAVED),
                        query.previousPeriodFrom(),
                        query.currentPeriodFrom(),
                        query.currentPeriodTo()
                );

        // 브랜치별 커밋 수 조회
        List<ProjectOverview.BranchCount> branches = projectQueryJpaRepository
                .countCommitsByBranch(
                        projectId,
                        ActivityType.PUSH_COMMIT,
                        query.currentFrom(),
                        query.currentTo()
                ).stream()
                .map(branch -> new ProjectOverview.BranchCount(
                        branch.getBranchName(), value(branch.getCount())
                ))
                .toList();

        // 오늘 회고 조회 (없으면 null)
        ProjectOverview.DailyReflection dailyReflection = projectQueryJpaRepository
                .findDailyReflections(
                        projectId,
                        ReflectionKind.DAILY,
                        query.today(),
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .map(reflection -> new ProjectOverview.DailyReflection(
                        reflection.getId(), reflection.getStatus()
                ))
                .orElse(null);

        // 최근 활동 조회 (최신순)
        List<ProjectOverview.RecentActivity> recentActivities = projectQueryJpaRepository
                .findRecentActivities(projectId, PageRequest.of(0, recentActivityLimit)).stream()
                .map(activity -> new ProjectOverview.RecentActivity(
                        activity.getId(),
                        activity.getActivityType(),
                        activity.getTitle(),
                        activity.getSummary(),
                        activity.getBranchName(),
                        activity.getCommitSha(),
                        activity.getFilesChanged(),
                        activity.getOccurredAt()
                ))
                .toList();

        // 이번 주 일일 회고 조회 (없으면 빈 리스트)
        List<ProjectOverview.Daily> weekDaily = projectQueryJpaRepository
                .findWeekDailyReflections(
                        projectId,
                        ReflectionKind.DAILY,
                        query.weekStart(),
                        query.weekEnd()
                ).stream()
                .map(reflection -> new ProjectOverview.Daily(
                        reflection.getPeriodStart(),
                        reflection.getStatus(),
                        reflection.getId()
                ))
                .toList();

        return new ProjectOverviewQueryResult(
                projectQueryJpaRepository.countAllActivities(projectId),
                value(activities.getCurrentEventCount()),
                value(activities.getPreviousEventCount()),
                value(activities.getCurrentCommitCount()),
                value(activities.getPreviousCommitCount()),
                branches,
                value(reflections.getCurrentDailySavedCount()),
                value(reflections.getPreviousDailySavedCount()),
                value(reflections.getCurrentWeeklyCount()),
                value(reflections.getPreviousWeeklyCount()),
                value(activities.getTodayActivityCount()),
                projectQueryJpaRepository.countDevlogs(projectId, query.today()) > 0,
                dailyReflection,
                recentActivities,
                weekDaily
        );
    }

    private List<ProjectJpaEntity> findProjectPage(
            Long userId,
            ProjectListStatus status,
            Set<Long> unlockedIds,
            OffsetDateTime cursorCreatedAt,
            Long cursorId,
            int size
    ) {
        // 페이지 요청 생성: size + 1을 요청하여 다음 페이지 존재 여부를 확인
        PageRequest page = PageRequest.of(0, size + 1);
        if (status == ProjectListStatus.ATTENTION_REQUIRED) {
            // 활성 상태이면서 동기화 실패 또는 경고 상태인 프로젝트를 조회
            return projectQueryJpaRepository.findAttentionRequiredPage(
                    userId,
                    ProjectStatus.ACTIVE,
                    ProjectSyncStatus.FAILED,
                    WARNING_WEBHOOK_STATUSES,
                    // project_limit은 1 이상이므로 일반적으로 비어 있지 않지만 IN 조건은 안전하게 보정
                    unlockedIds.isEmpty() ? Set.of(-1L) : unlockedIds,
                    cursorCreatedAt,
                    cursorId,
                    page
            );
        }

        // 활성 상태인 프로젝트를 조회
        return projectQueryJpaRepository.findActivePage(
                userId,
                ProjectStatus.ACTIVE,
                cursorCreatedAt,
                cursorId,
                page
        );
    }

    private Map<Long, Long> countMap(List<ProjectCountProjection> counts) {
        return counts.stream().collect(Collectors.toMap(
                ProjectCountProjection::getProjectId,
                ProjectCountProjection::getCount
        ));
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String encodeCursor(ProjectJpaEntity entity) {
        String raw = entity.getCreatedAt() + "|" + entity.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
