package seungyong.helpmebackend.webhook.application.port.out;

import seungyong.helpmebackend.activity.domain.entity.ActivitySeed;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.OffsetDateTime;
import java.util.List;

public interface InitialSyncPortOut {
    List<ActivitySeed> fetchActivities(
            Project project,
            String accessToken,
            OffsetDateTime since,
            int maxPages
    );
}
