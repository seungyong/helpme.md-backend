package seungyong.helpmebackend.notion.application.port.in.result;

import java.time.OffsetDateTime;

public record UpdatedNotionDefaultPage(
        String id,
        String title,
        OffsetDateTime updatedAt
) {
}
