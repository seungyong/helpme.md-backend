package seungyong.helpmebackend.project.adapter.out.persistence.projection;

public interface ProjectActivityOverviewProjection {
    Long getCurrentEventCount();

    Long getPreviousEventCount();

    Long getCurrentCommitCount();

    Long getPreviousCommitCount();

    Long getTodayActivityCount();
}
