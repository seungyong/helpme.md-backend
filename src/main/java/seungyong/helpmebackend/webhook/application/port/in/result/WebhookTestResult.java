package seungyong.helpmebackend.webhook.application.port.in.result;

import java.time.OffsetDateTime;

public record WebhookTestResult(
        String testId,
        String status,
        String deliveryId,
        OffsetDateTime receivedAt,
        OffsetDateTime processedAt,
        Error error
) {
    public record Error(String code, String message, boolean retryable) {
    }
}
