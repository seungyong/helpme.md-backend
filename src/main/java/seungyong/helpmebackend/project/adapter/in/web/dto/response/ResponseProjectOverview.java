package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.project.domain.entity.ProjectOverview;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ResponseProjectOverview(
        String healthStatus,
        Project project,
        ResponseProject.Sync sync,
        ResponseProject.Webhook webhook,
        Metrics metrics,
        Today today,
        List<RecentActivity> recentActivities,
        CurrentWeek currentWeek,
        NextGeneration nextGeneration
) {
    public static ResponseProjectOverview from(ProjectOverview result) {
        return new ResponseProjectOverview(
                result.healthStatus().getApiValue(),
                new Project(
                        result.project().getId(),
                        result.project().getRepoFullName(),
                        result.project().getSettings().trackedBranches(),
                        result.project().getSettings().timezone()
                ),
                ResponseProject.Sync.from(result.project().getSync()),
                ResponseProject.Webhook.from(result.project().getWebhook()),
                Metrics.from(result.metrics()),
                Today.from(result.today()),
                result.recentActivities().stream().map(RecentActivity::from).toList(),
                CurrentWeek.from(result.currentWeek()),
                new NextGeneration(
                        result.nextGeneration().dailyAt(),
                        result.nextGeneration().weeklyAt()
                )
        );
    }

    public record Project(
            Long id,
            String repoFullname,
            List<String> trackedBranches,
            String timezone
    ) {
    }

    public record Metrics(
            Comparison events7d,
            CommitComparison commits7d,
            Comparison dailySaved7d,
            Comparison weeklyCount
    ) {
        private static Metrics from(ProjectOverview.Metrics metrics) {
            return new Metrics(
                    Comparison.from(metrics.events7d()),
                    CommitComparison.from(metrics.commits7d()),
                    Comparison.from(metrics.dailySaved7d()),
                    Comparison.from(metrics.weeklyCount())
            );
        }
    }

    public record Comparison(long current, long previous, double changeRate) {
        private static Comparison from(ProjectOverview.Comparison comparison) {
            return new Comparison(
                    comparison.current(), comparison.previous(), comparison.changeRate()
            );
        }
    }

    public record CommitComparison(
            long current,
            long previous,
            double changeRate,
            List<BranchCount> byBranch
    ) {
        private static CommitComparison from(ProjectOverview.CommitComparison comparison) {
            return new CommitComparison(
                    comparison.current(),
                    comparison.previous(),
                    comparison.changeRate(),
                    comparison.byBranch().stream()
                            .map(value -> new BranchCount(value.branch(), value.count()))
                            .toList()
            );
        }
    }

    public record BranchCount(String branch, long count) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Today(
            LocalDate date,
            long activityCount,
            Devlog devlog,
            DailyReflection dailyReflection
    ) {
        private static Today from(ProjectOverview.Today today) {
            return new Today(
                    today.date(),
                    today.activityCount(),
                    new Devlog(today.devlogExists()),
                    DailyReflection.from(today.dailyReflection())
            );
        }
    }

    public record Devlog(boolean exists) {
    }

    public record DailyReflection(Long id, String status) {
        private static DailyReflection from(ProjectOverview.DailyReflection reflection) {
            return reflection == null ? null : new DailyReflection(
                    reflection.id(), reflection.status().getDatabaseValue()
            );
        }
    }

    public record RecentActivity(
            Long id,
            String activityType,
            String title,
            String summary,
            String branchName,
            String commitSha,
            Integer filesChanged,
            OffsetDateTime occurredAt
    ) {
        private static RecentActivity from(ProjectOverview.RecentActivity activity) {
            return new RecentActivity(
                    activity.id(),
                    activity.activityType().getDatabaseValue(),
                    activity.title(),
                    activity.summary(),
                    activity.branchName(),
                    activity.commitSha(),
                    activity.filesChanged(),
                    activity.occurredAt()
            );
        }
    }

    public record CurrentWeek(
            LocalDate periodStart,
            LocalDate periodEnd,
            long completedDailyCount,
            List<Daily> daily
    ) {
        private static CurrentWeek from(ProjectOverview.CurrentWeek week) {
            return new CurrentWeek(
                    week.periodStart(),
                    week.periodEnd(),
                    week.completedDailyCount(),
                    week.daily().stream().map(Daily::from).toList()
            );
        }
    }

    public record Daily(LocalDate date, String status, Long reflectionId) {
        private static Daily from(ProjectOverview.Daily daily) {
            return new Daily(
                    daily.date(), daily.status().getDatabaseValue(), daily.reflectionId()
            );
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record NextGeneration(OffsetDateTime dailyAt, OffsetDateTime weeklyAt) {
    }
}
