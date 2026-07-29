package seungyong.helpmebackend.project.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import seungyong.helpmebackend.global.adapter.out.persistence.converter.DatabaseEnumConverters;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "projects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_projects_user_repo",
                        columnNames = {"user_id", "repo_fullname"}
                )
        }
)
@Entity(name = "Project")
public class ProjectJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "repo_fullname", nullable = false, columnDefinition = "TEXT")
    private String repoFullName;

    @Column(name = "github_repo_id")
    private Long githubRepoId;

    @Column(name = "github_installation_id")
    private Long githubInstallationId;

    @Column(name = "default_branch", columnDefinition = "TEXT")
    private String defaultBranch;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tracked_branches", nullable = false)
    private String[] trackedBranches;

    @Column(name = "track_all_branches", nullable = false)
    private boolean trackAllBranches;

    @Column(name = "is_private", nullable = false)
    private boolean privateRepository;

    @Convert(converter = DatabaseEnumConverters.ProjectStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private ProjectStatus status;

    @Convert(converter = DatabaseEnumConverters.ProjectSyncStatusConverter.class)
    @Column(name = "sync_status", nullable = false, columnDefinition = "TEXT")
    private ProjectSyncStatus syncStatus;

    @Column(name = "sync_started_at")
    private OffsetDateTime syncStartedAt;

    @Column(name = "sync_completed_at")
    private OffsetDateTime syncCompletedAt;

    @Column(name = "sync_error_code", columnDefinition = "TEXT")
    private String syncErrorCode;

    @Column(name = "sync_error_message", columnDefinition = "TEXT")
    private String syncErrorMessage;

    @Convert(converter = DatabaseEnumConverters.ProjectWebhookStatusConverter.class)
    @Column(name = "webhook_status", nullable = false, columnDefinition = "TEXT")
    private ProjectWebhookStatus webhookStatus;

    @Column(name = "timezone", nullable = false, columnDefinition = "TEXT")
    private String timezone;

    @Column(name = "daily_enabled", nullable = false)
    private boolean dailyEnabled;

    @Column(name = "daily_generation_time", nullable = false)
    private LocalTime dailyGenerationTime;

    @Column(name = "weekly_enabled", nullable = false)
    private boolean weeklyEnabled;

    @Column(name = "weekly_generation_day", nullable = false)
    private short weeklyGenerationDay;

    @Column(name = "weekly_generation_time", nullable = false)
    private LocalTime weeklyGenerationTime;

    @Column(name = "webhook_payload_retention_days", nullable = false)
    private short webhookPayloadRetentionDays;

    @Column(name = "webhook_last_checked_at")
    private OffsetDateTime webhookLastCheckedAt;

    @Column(name = "webhook_last_received_at")
    private OffsetDateTime webhookLastReceivedAt;

    @Column(name = "webhook_last_delivery_id", columnDefinition = "TEXT")
    private String webhookLastDeliveryId;

    @Column(name = "webhook_error_code", columnDefinition = "TEXT")
    private String webhookErrorCode;

    @Column(name = "webhook_error_message", columnDefinition = "TEXT")
    private String webhookErrorMessage;

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
    public ProjectJpaEntity(
            Long id,
            UserJpaEntity user,
            String repoFullName,
            Long githubRepoId,
            Long githubInstallationId,
            String defaultBranch,
            String[] trackedBranches,
            Boolean trackAllBranches,
            Boolean privateRepository,
            ProjectStatus status,
            ProjectSyncStatus syncStatus,
            OffsetDateTime syncStartedAt,
            OffsetDateTime syncCompletedAt,
            String syncErrorCode,
            String syncErrorMessage,
            ProjectWebhookStatus webhookStatus,
            String timezone,
            Boolean dailyEnabled,
            LocalTime dailyGenerationTime,
            Boolean weeklyEnabled,
            Short weeklyGenerationDay,
            LocalTime weeklyGenerationTime,
            Short webhookPayloadRetentionDays,
            OffsetDateTime webhookLastCheckedAt,
            OffsetDateTime webhookLastReceivedAt,
            String webhookLastDeliveryId,
            String webhookErrorCode,
            String webhookErrorMessage,
            OffsetDateTime deletionRequestedAt,
            String deletionErrorCode,
            String deletionErrorMessage,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.user = user;
        this.repoFullName = repoFullName;
        this.githubRepoId = githubRepoId;
        this.githubInstallationId = githubInstallationId;
        this.defaultBranch = defaultBranch;
        this.trackedBranches = trackedBranches == null ? new String[0] : trackedBranches.clone();
        this.trackAllBranches = Boolean.TRUE.equals(trackAllBranches);
        this.privateRepository = Boolean.TRUE.equals(privateRepository);
        this.status = status == null ? ProjectStatus.ACTIVE : status;
        this.syncStatus = syncStatus == null ? ProjectSyncStatus.PENDING : syncStatus;
        this.syncStartedAt = syncStartedAt;
        this.syncCompletedAt = syncCompletedAt;
        this.syncErrorCode = syncErrorCode;
        this.syncErrorMessage = syncErrorMessage;
        this.webhookStatus = webhookStatus == null ? ProjectWebhookStatus.WAITING : webhookStatus;
        this.timezone = timezone == null ? "Asia/Seoul" : timezone;
        this.dailyEnabled = dailyEnabled == null || dailyEnabled;
        this.dailyGenerationTime = dailyGenerationTime == null ? LocalTime.of(23, 30) : dailyGenerationTime;
        this.weeklyEnabled = weeklyEnabled == null || weeklyEnabled;
        this.weeklyGenerationDay = weeklyGenerationDay == null ? (short) 0 : weeklyGenerationDay;
        this.weeklyGenerationTime = weeklyGenerationTime == null ? LocalTime.of(23, 50) : weeklyGenerationTime;
        this.webhookPayloadRetentionDays =
                webhookPayloadRetentionDays == null ? (short) 30 : webhookPayloadRetentionDays;
        this.webhookLastCheckedAt = webhookLastCheckedAt;
        this.webhookLastReceivedAt = webhookLastReceivedAt;
        this.webhookLastDeliveryId = webhookLastDeliveryId;
        this.webhookErrorCode = webhookErrorCode;
        this.webhookErrorMessage = webhookErrorMessage;
        this.deletionRequestedAt = deletionRequestedAt;
        this.deletionErrorCode = deletionErrorCode;
        this.deletionErrorMessage = deletionErrorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
