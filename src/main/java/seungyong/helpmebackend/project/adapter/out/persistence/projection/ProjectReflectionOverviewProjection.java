package seungyong.helpmebackend.project.adapter.out.persistence.projection;

public interface ProjectReflectionOverviewProjection {
    Long getCurrentDailySavedCount();

    Long getPreviousDailySavedCount();

    Long getCurrentWeeklyCount();

    Long getPreviousWeeklyCount();
}
