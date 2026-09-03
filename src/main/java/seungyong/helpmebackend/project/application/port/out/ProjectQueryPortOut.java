package seungyong.helpmebackend.project.application.port.out;

import seungyong.helpmebackend.project.application.port.out.query.ProjectOverviewQuery;
import seungyong.helpmebackend.project.application.port.out.result.ProjectListQueryResult;
import seungyong.helpmebackend.project.application.port.out.result.ProjectOverviewQueryResult;
import seungyong.helpmebackend.project.domain.type.ProjectListStatus;

import java.time.OffsetDateTime;

public interface ProjectQueryPortOut {
    ProjectListQueryResult findProjects(
            Long userId,
            int effectiveLimit,
            ProjectListStatus status,
            OffsetDateTime metricFrom,
            OffsetDateTime cursorCreatedAt,
            Long cursorId,
            int size
    );

    ProjectOverviewQueryResult findOverview(
            Long projectId,
            ProjectOverviewQuery query,
            int recentActivityLimit
    );
}
