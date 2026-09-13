package seungyong.helpmebackend.project.application.port.out;

import seungyong.helpmebackend.project.application.port.out.query.ProjectOverviewQuery;
import seungyong.helpmebackend.project.application.port.out.result.ProjectListQueryResult;
import seungyong.helpmebackend.project.application.port.out.result.ProjectOverviewQueryResult;
import seungyong.helpmebackend.project.domain.type.ProjectListStatus;
import seungyong.helpmebackend.global.application.pagination.CursorPagination;

import java.time.OffsetDateTime;

public interface ProjectQueryPortOut {
    ProjectListQueryResult findProjects(
            Long userId,
            int effectiveLimit,
            ProjectListStatus status,
            OffsetDateTime metricFrom,
            CursorPagination<OffsetDateTime> pagination
    );

    ProjectOverviewQueryResult findOverview(
            Long projectId,
            ProjectOverviewQuery query,
            int recentActivityLimit
    );
}
