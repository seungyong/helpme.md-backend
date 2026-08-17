package seungyong.helpmebackend.webhook.domain.entity;

import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;
import java.util.Map;

public record WebhookDelivery(
        Long id,
        Long projectId,
        String deliveryId,
        String eventName,
        String action,
        WebhookDeliveryStatus status,
        Map<String, Object> sanitizedPayload,
        OffsetDateTime payloadExpiresAt,
        OffsetDateTime payloadPurgedAt,
        short attempts,
        OffsetDateTime nextRetryAt,
        OffsetDateTime processingStartedAt,
        OffsetDateTime processedAt,
        String errorCode,
        String errorMessage,
        OffsetDateTime receivedAt,
        OffsetDateTime updatedAt
) {
    public static final String INITIAL_SYNC_EVENT = "_internal_initial_sync";
    public static final String WEBHOOK_TEST_EVENT = "_internal_webhook_test";

    public WebhookDelivery {
        sanitizedPayload = sanitizedPayload == null ? Map.of() : Map.copyOf(sanitizedPayload);
    }

    public boolean isInitialSync() {
        return INITIAL_SYNC_EVENT.equals(eventName);
    }

    public boolean isWebhookTest() {
        return WEBHOOK_TEST_EVENT.equals(eventName);
    }
}
