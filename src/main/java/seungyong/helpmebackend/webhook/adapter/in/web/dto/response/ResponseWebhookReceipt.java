package seungyong.helpmebackend.webhook.adapter.in.web.dto.response;

import seungyong.helpmebackend.webhook.application.port.in.result.WebhookReceiptResult;

public record ResponseWebhookReceipt(String status, String deliveryId, int projectCount) {
    public static ResponseWebhookReceipt from(WebhookReceiptResult result) {
        return new ResponseWebhookReceipt(
                result.status(), result.deliveryId(), result.projectCount()
        );
    }
}
