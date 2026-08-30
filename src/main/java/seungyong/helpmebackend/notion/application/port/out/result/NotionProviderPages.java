package seungyong.helpmebackend.notion.application.port.out.result;

import java.util.List;

public record NotionProviderPages(
        List<NotionProviderPage> items,
        String nextCursor,
        boolean hasNext
) {
    public NotionProviderPages {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
