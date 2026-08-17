package seungyong.helpmebackend.activity.domain.entity;

import java.util.List;

public record ActivityPage(
        List<Activity> items,
        Summary summary,
        String nextCursor,
        boolean hasNext,
        boolean filtersApplied
) {
    public ActivityPage {
        items = List.copyOf(items);
    }

    public record Summary(long pushCount, long commitCount, long filesChanged, long contributors) {
    }
}
