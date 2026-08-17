package seungyong.helpmebackend.webhook.application.port.in.result;

public record WebhookReceiptResult(String status, String deliveryId, int projectCount) {
}
