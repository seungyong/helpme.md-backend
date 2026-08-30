package seungyong.helpmebackend.notion.adapter.in.web.dto.response;

import seungyong.helpmebackend.notion.domain.entity.NotionPageCandidate;
import seungyong.helpmebackend.notion.domain.entity.NotionPageCandidates;

import java.time.OffsetDateTime;
import java.util.List;

public record ResponseNotionPages(
        List<Item> items,
        Page page
) {
    public static ResponseNotionPages from(NotionPageCandidates pages) {
        return new ResponseNotionPages(
                pages.items().stream().map(Item::from).toList(),
                new Page(pages.nextCursor(), pages.hasNext())
        );
    }

    public record Item(String id, String title, String path, OffsetDateTime lastEditedAt) {
        private static Item from(NotionPageCandidate page) {
            return new Item(page.id(), page.title(), page.path(), page.lastEditedAt());
        }
    }

    public record Page(String nextCursor, boolean hasNext) { }
}
