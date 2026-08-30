package seungyong.helpmebackend.notion.domain.entity;

import java.time.OffsetDateTime;
import java.util.Objects;

public record NotionAuthorization(String workspaceId, String workspaceName, String botId, String ownerName,
                                  String ownerEmail, String encryptedAccessToken, String encryptedRefreshToken,
                                  OffsetDateTime authorizedAt) {
    public NotionAuthorization(
            String workspaceId,
            String workspaceName,
            String botId,
            String ownerName,
            String ownerEmail,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            OffsetDateTime authorizedAt
    ) {
        this.workspaceId = requireText(workspaceId, "workspace id");
        this.workspaceName = workspaceName;
        this.botId = botId;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.encryptedAccessToken = requireText(encryptedAccessToken, "encrypted access token");
        this.encryptedRefreshToken = requireText(encryptedRefreshToken, "encrypted refresh token");
        this.authorizedAt = Objects.requireNonNull(authorizedAt, "authorizedAt");
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
