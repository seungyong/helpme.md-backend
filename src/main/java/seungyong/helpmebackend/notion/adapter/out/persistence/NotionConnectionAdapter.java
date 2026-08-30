package seungyong.helpmebackend.notion.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.notion.adapter.out.persistence.entity.NotionConnectionJpaEntity;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.notion.domain.entity.NotionAuthorization;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.notion.domain.exception.NotionErrorCode;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotionConnectionAdapter implements NotionConnectionPortOut {
    private final NotionConnectionJpaRepository notionConnectionJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<NotionConnection> getByUserId(Long userId) {
        return notionConnectionJpaRepository.findByUser_Id(userId).map(this::toDomain);
    }

    @Override
    @Transactional
    public NotionConnection saveAuthorization(
            Long userId, NotionAuthorization authorization
    ) {
        Optional<NotionConnectionJpaEntity> current =
                notionConnectionJpaRepository.findByUser_Id(userId);

        if (current.isPresent()
                && current.orElseThrow().belongsToWorkspace(authorization.workspaceId())) {
            NotionConnectionJpaEntity entity = current.orElseThrow();
            entity.reconnect(values(authorization));
            return toDomain(notionConnectionJpaRepository.saveAndFlush(entity));
        }

        if (current.isPresent()) {
            notionConnectionJpaRepository.delete(current.orElseThrow());
            notionConnectionJpaRepository.flush();
        }

        NotionConnectionJpaEntity entity = NotionConnectionJpaEntity.builder()
                .user(UserJpaEntity.builder().id(userId).build())
                .workspaceId(authorization.workspaceId())
                .workspaceName(authorization.workspaceName())
                .botId(authorization.botId())
                .ownerName(authorization.ownerName())
                .ownerEmail(authorization.ownerEmail())
                .encryptedAccessToken(authorization.encryptedAccessToken())
                .encryptedRefreshToken(authorization.encryptedRefreshToken())
                .tokenRefreshedAt(authorization.authorizedAt())
                .lastVerifiedAt(authorization.authorizedAt())
                .build();
        return toDomain(notionConnectionJpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public NotionConnection rotateTokens(
            Long userId,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            OffsetDateTime refreshedAt
    ) {
        NotionConnectionJpaEntity entity = getEntity(userId);
        entity.rotateTokens(encryptedAccessToken, encryptedRefreshToken, refreshedAt);
        return toDomain(notionConnectionJpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public NotionConnection recordVerification(Long userId, OffsetDateTime verifiedAt) {
        NotionConnectionJpaEntity entity = getEntity(userId);
        entity.recordVerification(verifiedAt);
        return toDomain(notionConnectionJpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public NotionConnection markReconnectRequired(
            Long userId,
            String errorCode,
            String errorMessage,
            OffsetDateTime changedAt
    ) {
        NotionConnectionJpaEntity entity = getEntity(userId);
        entity.markReconnectRequired(errorCode, errorMessage);
        return toDomain(notionConnectionJpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public NotionConnection updateDefaultPage(
            Long userId,
            String pageId,
            String pageTitle,
            OffsetDateTime changedAt
    ) {
        NotionConnectionJpaEntity entity = getEntity(userId);
        entity.changeDefaultPage(pageId, pageTitle);
        return toDomain(notionConnectionJpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        NotionConnectionJpaEntity entity = getEntity(userId);
        notionConnectionJpaRepository.delete(entity);
        notionConnectionJpaRepository.flush();
    }

    private NotionConnectionJpaEntity getEntity(Long userId) {
        return notionConnectionJpaRepository.findByUser_Id(userId)
                .orElseThrow(() -> new CustomException(
                        NotionErrorCode.NOTION_CONNECTION_NOT_FOUND
                ));
    }

    private NotionConnectionJpaEntity.NotionAuthorizationValues values(
            NotionAuthorization authorization
    ) {
        return new NotionConnectionJpaEntity.NotionAuthorizationValues(
                authorization.workspaceId(),
                authorization.workspaceName(),
                authorization.botId(),
                authorization.ownerName(),
                authorization.ownerEmail(),
                authorization.encryptedAccessToken(),
                authorization.encryptedRefreshToken(),
                authorization.authorizedAt()
        );
    }

    private NotionConnection toDomain(NotionConnectionJpaEntity entity) {
        return NotionConnection.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .workspaceId(entity.getWorkspaceId())
                .workspaceName(entity.getWorkspaceName())
                .botId(entity.getBotId())
                .ownerName(entity.getOwnerName())
                .ownerEmail(entity.getOwnerEmail())
                .encryptedAccessToken(entity.getEncryptedAccessToken())
                .encryptedRefreshToken(entity.getEncryptedRefreshToken())
                .tokenRefreshedAt(entity.getTokenRefreshedAt())
                .status(entity.getStatus())
                .defaultParentPageId(entity.getDefaultParentPageId())
                .defaultParentPageTitle(entity.getDefaultParentPageTitle())
                .lastVerifiedAt(entity.getLastVerifiedAt())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
