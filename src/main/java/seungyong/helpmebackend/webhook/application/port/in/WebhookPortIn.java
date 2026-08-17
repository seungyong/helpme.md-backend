package seungyong.helpmebackend.webhook.application.port.in;

import seungyong.helpmebackend.webhook.application.port.in.result.WebhookReceiptResult;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookTestResult;

public interface WebhookPortIn {
    WebhookReceiptResult receive(
            String signature,
            String eventName,
            String deliveryId,
            byte[] rawBody
    );

    WebhookTestResult startTest(Long userId, Long projectId);

    WebhookTestResult getTest(Long userId, Long projectId, String testId);
}
