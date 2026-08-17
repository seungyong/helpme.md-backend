package seungyong.helpmebackend.webhook.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookTestResult;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResponseWebhookTest(
        String testId,
        String status,
        String deliveryId,
        OffsetDateTime receivedAt,
        OffsetDateTime processedAt,
        Error error
) {
    public static ResponseWebhookTest from(WebhookTestResult result) {
        return new ResponseWebhookTest(
                result.testId(), result.status(), result.deliveryId(), result.receivedAt(),
                result.processedAt(), Error.from(result.error())
        );
    }

    public record Error(String code, String message, boolean retryable) {
        private static Error from(WebhookTestResult.Error error) {
            return error == null ? null
                    : new Error(error.code(), error.message(), error.retryable());
        }
    }
}
