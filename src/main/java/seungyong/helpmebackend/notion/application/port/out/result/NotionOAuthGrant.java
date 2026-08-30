package seungyong.helpmebackend.notion.application.port.out.result;

import lombok.Getter;

@Getter
public final class NotionOAuthGrant {
    private final String accessToken;
    private final String refreshToken;
    private final String botId;
    private final String workspaceId;
    private final String workspaceName;
    private final String ownerName;
    private final String ownerEmail;

    public NotionOAuthGrant(
            String accessToken,
            String refreshToken,
            String botId,
            String workspaceId,
            String workspaceName,
            String ownerName,
            String ownerEmail
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.botId = botId;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
    }
}
