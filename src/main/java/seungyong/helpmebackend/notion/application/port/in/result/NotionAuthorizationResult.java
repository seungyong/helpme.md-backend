package seungyong.helpmebackend.notion.application.port.in.result;

import java.time.OffsetDateTime;

public record NotionAuthorizationResult(
        String authorizationUrl,
        OffsetDateTime stateExpiresAt
) {
}
