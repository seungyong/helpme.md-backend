package seungyong.helpmebackend.webhook.application.port.out;

import seungyong.helpmebackend.webhook.domain.entity.WebhookDelivery;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public interface WebhookDeliveryPortOut {
    RegisterResult register(
            Long projectId,
            String deliveryId,
            String eventName,
            String action,
            Map<String, Object> sanitizedPayload,
            OffsetDateTime payloadExpiresAt
    );

    Optional<WebhookDelivery> claimNext(OffsetDateTime now, OffsetDateTime stuckBefore);

    void complete(Long id, boolean ignored, OffsetDateTime processedAt);

    void fail(
            Long id,
            String errorCode,
            String errorMessage,
            OffsetDateTime nextRetryAt,
            boolean terminal
    );

    void recordTestDelivery(Long id, String deliveryId);

    Optional<WebhookDelivery> getTest(Long projectId, String testId);

    RegisterResult registerTest(Long projectId, String testId, OffsetDateTime deadline);

    int expireTimedOutTests(OffsetDateTime now, String errorCode, String errorMessage);

    int purgeExpiredPayloads(OffsetDateTime now);

    record RegisterResult(WebhookDelivery delivery, boolean created) {
    }
}
