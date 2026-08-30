package seungyong.helpmebackend.notion.domain.entity;

import lombok.Builder;
import lombok.Getter;
import seungyong.helpmebackend.notion.domain.type.NotionConnectionStatus;

import java.time.OffsetDateTime;

@Getter
@Builder
public class NotionConnection {
    private final Long id;
    private final Long userId;
    private final String workspaceId;
    private final String workspaceName;
    private final String botId;
    private final String ownerName;
    private final String ownerEmail;
    private final String encryptedAccessToken;
    private final String encryptedRefreshToken;
    private final OffsetDateTime tokenRefreshedAt;
    private final NotionConnectionStatus status;
    private final String defaultParentPageId;
    private final String defaultParentPageTitle;
    private final OffsetDateTime lastVerifiedAt;
    private final String errorCode;
    private final String errorMessage;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public static NotionConnection disconnected(Long userId) {
        return NotionConnection.builder().userId(userId).build();
    }

    public boolean exists() {
        return id != null;
    }

    public boolean isConnected() {
        return status == NotionConnectionStatus.CONNECTED;
    }

    public boolean isSameWorkspace(String targetWorkspaceId) {
        return workspaceId != null && workspaceId.equals(targetWorkspaceId);
    }

    public boolean hasTokenPair() {
        return encryptedAccessToken != null && !encryptedAccessToken.isBlank()
                && encryptedRefreshToken != null && !encryptedRefreshToken.isBlank();
    }
}
