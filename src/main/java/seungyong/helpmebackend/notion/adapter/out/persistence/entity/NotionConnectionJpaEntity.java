package seungyong.helpmebackend.notion.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import seungyong.helpmebackend.global.adapter.out.persistence.converter.DatabaseEnumConverters;
import seungyong.helpmebackend.notion.domain.type.NotionConnectionStatus;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notion_connections")
@Entity(name = "NotionConnection")
public class NotionConnectionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserJpaEntity user;

    @Column(name = "workspace_id", nullable = false, columnDefinition = "TEXT")
    private String workspaceId;

    @Column(name = "workspace_name", columnDefinition = "TEXT")
    private String workspaceName;

    @Column(name = "bot_id", columnDefinition = "TEXT")
    private String botId;

    @Column(name = "owner_name", columnDefinition = "TEXT")
    private String ownerName;

    @Column(name = "owner_email", columnDefinition = "TEXT")
    private String ownerEmail;

    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "token_refreshed_at")
    private OffsetDateTime tokenRefreshedAt;

    @Convert(converter = DatabaseEnumConverters.NotionConnectionStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private NotionConnectionStatus status;

    @Column(name = "default_parent_page_id", columnDefinition = "TEXT")
    private String defaultParentPageId;

    @Column(name = "default_parent_page_title", columnDefinition = "TEXT")
    private String defaultParentPageTitle;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;

    @Column(name = "error_code", columnDefinition = "TEXT")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public NotionConnectionJpaEntity(
            Long id,
            UserJpaEntity user,
            String workspaceId,
            String workspaceName,
            String botId,
            String ownerName,
            String ownerEmail,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            OffsetDateTime tokenRefreshedAt,
            NotionConnectionStatus status,
            String defaultParentPageId,
            String defaultParentPageTitle,
            OffsetDateTime lastVerifiedAt,
            String errorCode,
            String errorMessage,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.user = user;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.botId = botId;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.tokenRefreshedAt = tokenRefreshedAt;
        this.status = status == null ? NotionConnectionStatus.CONNECTED : status;
        this.defaultParentPageId = defaultParentPageId;
        this.defaultParentPageTitle = defaultParentPageTitle;
        this.lastVerifiedAt = lastVerifiedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
