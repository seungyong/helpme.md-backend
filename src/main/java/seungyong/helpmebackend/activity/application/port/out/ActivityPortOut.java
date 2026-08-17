package seungyong.helpmebackend.activity.application.port.out;

import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;
import seungyong.helpmebackend.activity.domain.type.ActivityType;

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
            OffsetDateTime cursorOccurredAt,
            Long cursorId,
            int size,
            boolean filtersApplied
    );
}
