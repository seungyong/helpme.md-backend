package seungyong.helpmebackend.notion.application.port.out;

import seungyong.helpmebackend.notion.domain.entity.NotionAuthorization;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface NotionConnectionPortOut {
    Optional<NotionConnection> getByUserId(Long userId);

    NotionConnection saveAuthorization(Long userId, NotionAuthorization authorization);

    NotionConnection rotateTokens(
            Long userId,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            OffsetDateTime refreshedAt
    );

    NotionConnection recordVerification(Long userId, OffsetDateTime verifiedAt);

    NotionConnection markReconnectRequired(
            Long userId, String errorCode, String errorMessage, OffsetDateTime changedAt
    );

    NotionConnection updateDefaultPage(
            Long userId, String pageId, String pageTitle, OffsetDateTime changedAt
    );

    void deleteByUserId(Long userId);
}
