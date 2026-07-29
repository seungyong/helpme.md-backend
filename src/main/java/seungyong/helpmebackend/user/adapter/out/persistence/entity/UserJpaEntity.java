package seungyong.helpmebackend.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import seungyong.helpmebackend.global.adapter.out.persistence.converter.DatabaseEnumConverters;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.adapter.out.persistence.converter.EncryptedTokenConverter;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;
import seungyong.helpmebackend.user.domain.type.PlanCode;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Entity(name = "User")
public class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Convert(converter = EncryptedTokenConverter.class)
    @Column(name = "github_token", nullable = false, unique = true, columnDefinition = "TEXT")
    private EncryptedToken githubToken;

    @Convert(converter = DatabaseEnumConverters.PlanCodeConverter.class)
    @Column(name = "plan_code", nullable = false, columnDefinition = "TEXT")
    private PlanCode planCode;

    @Column(name = "project_limit", nullable = false)
    private Short projectLimit;

    @Column(name = "plan_expires_at")
    private OffsetDateTime planExpiresAt;

    @Convert(converter = DatabaseEnumConverters.UserStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private UserStatus status;

    @Convert(converter = DatabaseEnumConverters.GithubTokenStatusConverter.class)
    @Column(name = "github_token_status", nullable = false, columnDefinition = "TEXT")
    private GithubTokenStatus githubTokenStatus;

    @Column(name = "github_token_verified_at")
    private OffsetDateTime githubTokenVerifiedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "deletion_requested_at")
    private OffsetDateTime deletionRequestedAt;

    @Column(name = "deletion_error_code", columnDefinition = "TEXT")
    private String deletionErrorCode;

    @Column(name = "deletion_error_message", columnDefinition = "TEXT")
    private String deletionErrorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public UserJpaEntity(
            Long id,
            String name,
            Long githubId,
            EncryptedToken githubToken,
            PlanCode planCode,
            Short projectLimit,
            OffsetDateTime planExpiresAt,
            UserStatus status,
            GithubTokenStatus githubTokenStatus,
            OffsetDateTime githubTokenVerifiedAt,
            OffsetDateTime lastLoginAt,
            OffsetDateTime deletionRequestedAt,
            String deletionErrorCode,
            String deletionErrorMessage,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.githubId = githubId;
        this.githubToken = githubToken;
        this.planCode = planCode == null ? PlanCode.FREE : planCode;
        this.projectLimit = projectLimit == null ? (short) 1 : projectLimit;
        this.planExpiresAt = planExpiresAt;
        this.status = status == null ? UserStatus.ACTIVE : status;
        this.githubTokenStatus = githubTokenStatus == null ? GithubTokenStatus.UNKNOWN : githubTokenStatus;
        this.githubTokenVerifiedAt = githubTokenVerifiedAt;
        this.lastLoginAt = lastLoginAt;
        this.deletionRequestedAt = deletionRequestedAt;
        this.deletionErrorCode = deletionErrorCode;
        this.deletionErrorMessage = deletionErrorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
