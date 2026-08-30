package seungyong.helpmebackend.notion.adapter.in.web.dto.response;

import seungyong.helpmebackend.notion.application.port.in.result.NotionAuthorizationResult;

import java.time.OffsetDateTime;

public record ResponseNotionAuthorization(
        String authorizationUrl,
        OffsetDateTime stateExpiresAt
) {
    public static ResponseNotionAuthorization from(NotionAuthorizationResult result) {
        return new ResponseNotionAuthorization(result.authorizationUrl(), result.stateExpiresAt());
    }
}
