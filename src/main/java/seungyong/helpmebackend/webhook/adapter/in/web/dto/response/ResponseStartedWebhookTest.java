package seungyong.helpmebackend.webhook.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookTestResult;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResponseStartedWebhookTest(
        String testId,
        String deliveryId,
        String status,
        String location,
        int retryAfterSeconds
) {
    public static ResponseStartedWebhookTest from(Long projectId, WebhookTestResult result) {
        return new ResponseStartedWebhookTest(
                result.testId(), result.deliveryId(), result.status(),
                "/api/v1/projects/%d/webhook-tests/%s".formatted(projectId, result.testId()),
                2
        );
    }
}
