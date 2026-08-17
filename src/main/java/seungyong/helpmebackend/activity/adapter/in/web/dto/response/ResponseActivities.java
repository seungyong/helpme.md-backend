package seungyong.helpmebackend.activity.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ResponseActivities(
        List<Item> items,
        Summary summary,
        Page page,
        boolean filtersApplied
) {
    public static ResponseActivities from(ActivityPage result) {
        return new ResponseActivities(
                result.items().stream().map(Item::from).toList(),
                Summary.from(result.summary()),
                new Page(result.nextCursor(), result.hasNext()),
                result.filtersApplied()
        );
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Item(
            Long id,
            String type,
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
        private static Item from(Activity activity) {
            return new Item(
                    activity.id(), activity.type().getDatabaseValue(), activity.branchName(),
                    activity.commitSha(), activity.title(), activity.summary(), activity.actorLogin(),
                    activity.publicUrl(), activity.additions(), activity.deletions(),
                    activity.filesChanged(), activity.occurredAt(), activity.details()
            );
        }
    }

    public record Summary(long pushCount, long commitCount, long filesChanged, long contributors) {
        private static Summary from(ActivityPage.Summary summary) {
            return new Summary(
                    summary.pushCount(), summary.commitCount(),
                    summary.filesChanged(), summary.contributors()
            );
        }
    }

    public record Page(String nextCursor, boolean hasNext) {
    }
}
