package seungyong.helpmebackend.notion.application.port.out.result;

import java.time.OffsetDateTime;

public record NotionProviderPage(
        String id,
        String title,
        OffsetDateTime lastEditedAt
) {
}
