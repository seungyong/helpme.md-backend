package seungyong.helpmebackend.notion.domain.entity;

import java.util.List;

public record NotionPageCandidates(
        List<NotionPageCandidate> items,
        String nextCursor,
        boolean hasNext
) {
    public NotionPageCandidates {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
