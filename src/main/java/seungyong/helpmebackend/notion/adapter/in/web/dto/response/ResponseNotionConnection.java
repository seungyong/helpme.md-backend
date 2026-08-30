package seungyong.helpmebackend.notion.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResponseNotionConnection(
        boolean connected,
        String status,
        Workspace workspace,
        Owner owner,
        ParentPage defaultParentPage,
        OffsetDateTime lastVerifiedAt,
        PublicError error
) {
    public static ResponseNotionConnection from(NotionConnection connection) {
        if (!connection.exists()) {
            return new ResponseNotionConnection(false, null, null, null, null, null, null);
        }
        PublicError error = connection.getErrorCode() == null ? null
                : new PublicError(connection.getErrorCode(), publicMessage(connection), false);
        return new ResponseNotionConnection(
                connection.isConnected(),
                connection.getStatus().getDatabaseValue(),
                new Workspace(connection.getWorkspaceId(), connection.getWorkspaceName()),
                new Owner(connection.getOwnerName(), connection.getOwnerEmail()),
                connection.getDefaultParentPageId() == null ? null : new ParentPage(
                        connection.getDefaultParentPageId(),
                        connection.getDefaultParentPageTitle()
                ),
                connection.getLastVerifiedAt(), error
        );
    }

    private static String publicMessage(NotionConnection connection) {
        return "Notion 연결 상태를 확인해 주세요.";
    }

    public record Workspace(String id, String name) { }
    public record Owner(String name, String email) { }
    public record ParentPage(String id, String title) { }
    public record PublicError(String code, String message, boolean retryable) { }
}
