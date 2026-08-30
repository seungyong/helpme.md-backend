package seungyong.helpmebackend.notion.domain.entity;

import java.time.OffsetDateTime;

public record NotionPageCandidate(
        String id,
        String title,
        String path,
        OffsetDateTime lastEditedAt
) {
}
