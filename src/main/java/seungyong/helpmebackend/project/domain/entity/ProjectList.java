package seungyong.helpmebackend.project.domain.entity;

import java.util.List;

public record ProjectList(
        Plan plan,
        List<Item> items,
        Page page
) {
    public ProjectList {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Plan(String code, int limit, long used) {
    }

    public record Item(
            Project project,
            boolean locked,
            boolean attentionRequired,
            Metrics metrics
    ) {
    }

    public record Metrics(
            long eventCount7d,
            long completedReflectionCount,
            String lastActivityTitle
    ) {
    }

    public record Page(String nextCursor, boolean hasNext) {
    }
}
