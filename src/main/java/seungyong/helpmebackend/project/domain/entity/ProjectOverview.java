package seungyong.helpmebackend.project.domain.entity;

import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.project.domain.type.ProjectHealthStatus;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ProjectOverview(
        ProjectHealthStatus healthStatus,
        Project project,
        Metrics metrics,
        Today today,
        List<RecentActivity> recentActivities,
        CurrentWeek currentWeek,
        NextGeneration nextGeneration
) {
    public ProjectOverview {
        recentActivities = recentActivities == null ? List.of() : List.copyOf(recentActivities);
    }

    public record Metrics(
            Comparison events7d,
            CommitComparison commits7d,
            Comparison dailySaved7d,
            Comparison weeklyCount
    ) {
    }

    public record Comparison(long current, long previous, double changeRate) {
    }

    public record CommitComparison(
            long current,
            long previous,
            double changeRate,
            List<BranchCount> byBranch
    ) {
        public CommitComparison {
            byBranch = byBranch == null ? List.of() : List.copyOf(byBranch);
        }
    }

    public record BranchCount(String branch, long count) {
    }

    public record Today(
            LocalDate date,
            long activityCount,
            boolean devlogExists,
            DailyReflection dailyReflection
    ) {
    }

    public record DailyReflection(Long id, ReflectionStatus status) {
    }

    public record RecentActivity(
            Long id,
            ActivityType activityType,
            String title,
            String summary,
            String branchName,
            String commitSha,
            Integer filesChanged,
            OffsetDateTime occurredAt
    ) {
    }

    public record CurrentWeek(
            LocalDate periodStart,
            LocalDate periodEnd,
            long completedDailyCount,
            List<Daily> daily
    ) {
        public CurrentWeek {
            daily = daily == null ? List.of() : List.copyOf(daily);
        }
    }

    public record Daily(LocalDate date, ReflectionStatus status, Long reflectionId) {
    }

    public record NextGeneration(OffsetDateTime dailyAt, OffsetDateTime weeklyAt) {
    }
}
