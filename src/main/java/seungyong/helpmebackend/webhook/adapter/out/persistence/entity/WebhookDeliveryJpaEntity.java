package seungyong.helpmebackend.webhook.adapter.out.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "webhook_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "webhook_deliveries_project_delivery_uk",
                columnNames = {"project_id", "delivery_id"}
        )
)
@Entity(name = "WebhookDelivery")
public class WebhookDeliveryJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectJpaEntity project;

    @Column(name = "delivery_id", nullable = false, columnDefinition = "TEXT")
    private String deliveryId;

    @Column(name = "event_name", nullable = false, columnDefinition = "TEXT")
    private String eventName;

    @Column(name = "action", columnDefinition = "TEXT")
    private String action;

    @Convert(converter = DatabaseEnumConverters.WebhookDeliveryStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private WebhookDeliveryStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sanitized_payload")
    private JsonNode sanitizedPayload;

    @Column(name = "payload_expires_at")
    private OffsetDateTime payloadExpiresAt;

    @Column(name = "payload_purged_at")
    private OffsetDateTime payloadPurgedAt;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "error_code", columnDefinition = "TEXT")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public WebhookDeliveryJpaEntity(
            Long id,
            ProjectJpaEntity project,
            String deliveryId,
            String eventName,
            String action,
            WebhookDeliveryStatus status,
            JsonNode sanitizedPayload,
            OffsetDateTime payloadExpiresAt,
            OffsetDateTime payloadPurgedAt,
            Short attempts,
            OffsetDateTime nextRetryAt,
            OffsetDateTime processingStartedAt,
            OffsetDateTime processedAt,
            String errorCode,
            String errorMessage,
            OffsetDateTime receivedAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.project = project;
        this.deliveryId = deliveryId;
        this.eventName = eventName;
        this.action = action;
        this.status = status == null ? WebhookDeliveryStatus.RECEIVED : status;
        this.sanitizedPayload = sanitizedPayload;
        this.payloadExpiresAt = payloadExpiresAt;
        this.payloadPurgedAt = payloadPurgedAt;
        this.attempts = attempts == null ? (short) 0 : attempts;
        this.nextRetryAt = nextRetryAt;
        this.processingStartedAt = processingStartedAt;
        this.processedAt = processedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.receivedAt = receivedAt;
        this.updatedAt = updatedAt;
    }

    public void claim(OffsetDateTime startedAt) {
        this.status = WebhookDeliveryStatus.PROCESSING;
        this.processingStartedAt = startedAt;
        this.attempts++;
        this.nextRetryAt = null;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void complete(boolean ignored, OffsetDateTime completedAt) {
        this.status = ignored ? WebhookDeliveryStatus.IGNORED : WebhookDeliveryStatus.PROCESSED;
        this.processedAt = completedAt;
        this.processingStartedAt = null;
        this.nextRetryAt = null;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void fail(String code, String message, OffsetDateTime retryAt) {
        this.status = WebhookDeliveryStatus.FAILED;
        this.processingStartedAt = null;
        this.nextRetryAt = retryAt;
        this.errorCode = code;
        this.errorMessage = message;
    }

    public void reset() {
        this.status = WebhookDeliveryStatus.RECEIVED;
        this.attempts = 0;
        this.nextRetryAt = null;
        this.processingStartedAt = null;
        this.processedAt = null;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void recordRelatedDelivery(String relatedDeliveryId) {
        this.action = relatedDeliveryId;
    }

    public void purgePayload(OffsetDateTime purgedAt) {
        this.sanitizedPayload = null;
        this.payloadPurgedAt = purgedAt;
    }
}
