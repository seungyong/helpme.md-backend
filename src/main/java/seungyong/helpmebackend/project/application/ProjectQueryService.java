package seungyong.helpmebackend.project.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.in.ProjectQueryPortIn;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectQueryPortOut;
import seungyong.helpmebackend.project.application.port.out.query.ProjectOverviewQuery;
import seungyong.helpmebackend.project.application.port.out.result.ProjectListQueryResult;
import seungyong.helpmebackend.project.application.port.out.result.ProjectOverviewQueryResult;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectList;
import seungyong.helpmebackend.project.domain.entity.ProjectOverview;
import seungyong.helpmebackend.project.domain.type.ProjectHealthStatus;
import seungyong.helpmebackend.project.domain.type.ProjectListStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ProjectQueryService implements ProjectQueryPortIn {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int RECENT_ACTIVITY_LIMIT = 5;

    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectPortOut projectPortOut;
    private final ProjectQueryPortOut projectQueryPortOut;
    private final UserPortOut userPortOut;

    @Override
    @Transactional(readOnly = true)
    public ProjectList getProjects(
            Long userId,
            String cursor,
            Integer size,
            String status
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        User user = userPortOut.getById(userId);
        int effectiveLimit = user.getPlan().effectiveProjectLimit(now);
        int normalizedSize = normalizeSize(size);
        ProjectListStatus normalizedStatus = normalizeStatus(status);
        Cursor decodedCursor = decodeCursor(cursor);

        ProjectListQueryResult result = projectQueryPortOut.findProjects(
                userId,
                effectiveLimit,
                normalizedStatus,
                now.minusDays(7),
                decodedCursor.createdAt(),
                decodedCursor.id(),
                normalizedSize
        );
        return new ProjectList(
                new ProjectList.Plan(
                        user.getPlan().code().getDatabaseValue(),
                        effectiveLimit,
                        projectPortOut.countByUserId(userId)
                ),
                result.items(),
                new ProjectList.Page(result.nextCursor(), result.hasNext())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectOverview getOverview(Long userId, Long projectId) {
        Project project = projectAccessResolver.resolveActive(userId, projectId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ZoneId zoneId = ZoneId.of(project.getSettings().timezone());
        ZonedDateTime localNow = now.atZoneSameInstant(zoneId);

        /*
        * 예시
        * today = 2026-09-03 (목)
        * currentPeriodFrom = 2026-08-28 (금) -> 28일부터 오늘까지의 활동 집계 (7일) -> 오늘로부터 6일 전까지의 활동 집계
        * previousPeriodFrom = 2026-08-21 (금) -> 21일부터 27일까지의 활동 집계 (7일) -> 오늘로부터 13일 전부터 7일 전까지의 활동 집계
        * */
        LocalDate today = localNow.toLocalDate();
        LocalDate currentPeriodFrom = today.minusDays(6);
        LocalDate previousPeriodFrom = currentPeriodFrom.minusDays(7);

        // 프로젝트 설정에서 지정한 요일을 기준으로 이번 주의 마지막 날(주간 회고 생성일)을 계산
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(
                toDayOfWeek(project.getSettings().weekly().generationDay())
        ));
        LocalDate weekStart = weekEnd.minusDays(6);

        ProjectOverviewQuery query = new ProjectOverviewQuery(
                previousPeriodFrom.atStartOfDay(zoneId).toOffsetDateTime(),
                currentPeriodFrom.atStartOfDay(zoneId).toOffsetDateTime(),
                today.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime(),
                today.atStartOfDay(zoneId).toOffsetDateTime(),
                today.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime(),
                previousPeriodFrom,
                currentPeriodFrom,
                today,
                weekStart,
                weekEnd,
                today
        );
        ProjectOverviewQueryResult result = projectQueryPortOut.findOverview(
                projectId, query, RECENT_ACTIVITY_LIMIT
        );

        return new ProjectOverview(
                healthStatus(project, result.totalActivityCount()),
                project,
                metrics(result),
                new ProjectOverview.Today(
                        today,
                        result.todayActivityCount(),
                        result.devlogExists(),
                        result.dailyReflection()
                ),
                result.recentActivities(),
                new ProjectOverview.CurrentWeek(
                        weekStart,
                        weekEnd,
                        result.weekDailyReflections().stream()
                                .filter(daily -> daily.status() == ReflectionStatus.SAVED)
                                .count(),
                        result.weekDailyReflections()
                ),
                new ProjectOverview.NextGeneration(
                        nextDailyGeneration(project, localNow),
                        nextWeeklyGeneration(project, localNow)
                )
        );
    }

    private ProjectOverview.Metrics metrics(ProjectOverviewQueryResult result) {
        return new ProjectOverview.Metrics(
                comparison(result.currentEventCount(), result.previousEventCount()),
                new ProjectOverview.CommitComparison(
                        result.currentCommitCount(),
                        result.previousCommitCount(),
                        changeRate(result.currentCommitCount(), result.previousCommitCount()),
                        result.commitByBranch()
                ),
                comparison(
                        result.currentDailySavedCount(), result.previousDailySavedCount()
                ),
                comparison(result.currentWeeklyCount(), result.previousWeeklyCount())
        );
    }

    private ProjectOverview.Comparison comparison(long current, long previous) {
        return new ProjectOverview.Comparison(current, previous, changeRate(current, previous));
    }

    private double changeRate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : 100.0;
        }
        double rate = (current - previous) * 100.0 / previous;
        return Math.round(rate * 10.0) / 10.0;
    }

    private ProjectHealthStatus healthStatus(Project project, long activityCount) {
        if (project.getSync().status() == ProjectSyncStatus.PENDING
                || project.getSync().status() == ProjectSyncStatus.RUNNING) {
            return ProjectHealthStatus.SYNCING;
        }
        if (project.getSync().status() == ProjectSyncStatus.FAILED
                || project.getWebhook().status() == ProjectWebhookStatus.DEGRADED
                || project.getWebhook().status() == ProjectWebhookStatus.DISCONNECTED) {
            return ProjectHealthStatus.WARNING;
        }
        return activityCount == 0
                ? ProjectHealthStatus.NO_EVENTS : ProjectHealthStatus.HEALTHY;
    }

    private OffsetDateTime nextDailyGeneration(Project project, ZonedDateTime localNow) {
        if (!project.getSettings().daily().enabled()) {
            return null;
        }
        ZonedDateTime candidate = localNow.toLocalDate()
                .atTime(project.getSettings().daily().generationTime())
                .atZone(localNow.getZone());
        if (!candidate.isAfter(localNow)) {
            candidate = candidate.plusDays(1);
        }
        return candidate.toOffsetDateTime();
    }

    private OffsetDateTime nextWeeklyGeneration(Project project, ZonedDateTime localNow) {
        if (!project.getSettings().weekly().enabled()) {
            return null;
        }

        DayOfWeek targetDay = toDayOfWeek(
                project.getSettings().weekly().generationDay()
        );

        // 오늘이 targetDay보다 늦은 경우, 이번 주의 targetDay를 찾기 위해 nextOrSame를 사용
        LocalDate targetDate = localNow.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(targetDay));

        // 날짜와 시간을 결합하여 ZonedDateTime 생성
        ZonedDateTime candidate = targetDate
                .atTime(project.getSettings().weekly().generationTime())
                .atZone(localNow.getZone());

        // 만약 candidate가 현재 시간보다 이전이라면, 다음 주의 targetDay로 이동
        if (!candidate.isAfter(localNow)) {
            candidate = candidate.plusWeeks(1);
        }

        return candidate.toOffsetDateTime();
    }

    private DayOfWeek toDayOfWeek(ReflectionWeekday weekday) {
        return weekday == ReflectionWeekday.SUNDAY
                ? DayOfWeek.SUNDAY : DayOfWeek.of(weekday.getDatabaseValue());
    }

    private int normalizeSize(Integer size) {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < 1 || value > MAX_SIZE) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        return value;
    }

    private ProjectListStatus normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return ProjectListStatus.ACTIVE;
        }
        try {
            return ProjectListStatus.fromApiValue(status.trim());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private Cursor decodeCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return new Cursor(null, null);
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", 2);
            return new Cursor(OffsetDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private record Cursor(OffsetDateTime createdAt, Long id) {
    }
}
