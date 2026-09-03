package seungyong.helpmebackend.project.application.port.out.result;

import seungyong.helpmebackend.project.domain.entity.ProjectOverview;

import java.util.List;

public record ProjectOverviewQueryResult(
        long totalActivityCount,
        long currentEventCount,
        long previousEventCount,
        long currentCommitCount,
        long previousCommitCount,
        List<ProjectOverview.BranchCount> commitByBranch,
        long currentDailySavedCount,
        long previousDailySavedCount,
        long currentWeeklyCount,
        long previousWeeklyCount,
        long todayActivityCount,
        boolean devlogExists,
        ProjectOverview.DailyReflection dailyReflection,
        List<ProjectOverview.RecentActivity> recentActivities,
        List<ProjectOverview.Daily> weekDailyReflections
) {
    public ProjectOverviewQueryResult {
        commitByBranch = commitByBranch == null ? List.of() : List.copyOf(commitByBranch);
        recentActivities = recentActivities == null ? List.of() : List.copyOf(recentActivities);
        weekDailyReflections = weekDailyReflections == null
                ? List.of() : List.copyOf(weekDailyReflections);
    }
}
