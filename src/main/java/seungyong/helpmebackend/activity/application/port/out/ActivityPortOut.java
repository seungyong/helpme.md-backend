package seungyong.helpmebackend.activity.application.port.out;

import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivityEvidenceBatch;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.application.pagination.CursorPagination;

import java.time.OffsetDateTime;
import java.util.List;

public interface ActivityPortOut {
    boolean saveIfAbsent(Activity activity);

    int saveAllIfAbsent(List<Activity> activities);

    ActivityPage findActivities(
            Long projectId,
            String query,
            String branch,
            ActivityType type,
            OffsetDateTime from,
            OffsetDateTime to,
            CursorPagination<OffsetDateTime> pagination,
            boolean filtersApplied
    );

    ActivityEvidenceBatch findEvidence(
            Long projectId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit
    );
}
