package seungyong.helpmebackend.activity.domain.entity;

import lombok.Builder;
import seungyong.helpmebackend.activity.domain.type.ActivityType;

import java.time.OffsetDateTime;
import java.util.Map;

@Builder
public record ActivitySeed(
        String externalKey,
        ActivityType type,
        String branchName,
        String commitSha,
        String title,
        String summary,
        String actorLogin,
        String publicUrl,
        Integer additions,
        Integer deletions,
        Integer filesChanged,
        OffsetDateTime occurredAt,
        Map<String, Object> details
) {
    public ActivitySeed {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
