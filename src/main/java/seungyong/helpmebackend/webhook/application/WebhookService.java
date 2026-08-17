package seungyong.helpmebackend.webhook.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.webhook.application.port.in.WebhookPortIn;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookReceiptResult;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookTestResult;
import seungyong.helpmebackend.webhook.application.port.out.WebhookDeliveryPortOut;
import seungyong.helpmebackend.webhook.application.support.WebhookPayloadSanitizer;
import seungyong.helpmebackend.webhook.application.support.WebhookSignatureVerifier;
import seungyong.helpmebackend.webhook.domain.entity.WebhookDelivery;
import seungyong.helpmebackend.webhook.domain.exception.WebhookErrorCode;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookService implements WebhookPortIn {
    private static final int TEST_TIMEOUT_SECONDS = 30;

    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookPayloadSanitizer payloadSanitizer;
    private final WebhookDeliveryPortOut webhookDeliveryPortOut;
    private final ProjectPortOut projectPortOut;
    private final ProjectAccessResolver projectAccessResolver;

    @Override
    public WebhookReceiptResult receive(
            String signature,
            String eventName,
            String deliveryId,
            byte[] rawBody
    ) {
        signatureVerifier.verify(signature, rawBody);
        if (!StringUtils.hasText(eventName) || !StringUtils.hasText(deliveryId)) {
            throw new CustomException(WebhookErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
        WebhookPayloadSanitizer.SanitizedPayload payload = payloadSanitizer.sanitize(
                eventName, rawBody
        );
        List<Project> projects = projectPortOut.getActiveByGithubRepository(
                payload.installationId(), payload.repositoryId()
        );
        if (projects.isEmpty()) {
            return new WebhookReceiptResult("ignored", deliveryId, 0);
        }

        OffsetDateTime receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
        int created = 0;
        for (Project project : projects) {
            // Webhook payload retention 기간을 계산하여 expiresAt 설정
            OffsetDateTime expiresAt = receivedAt.plusDays(
                    project.getSettings().webhookPayloadRetentionDays()
            );

            // WebhookDelivery를 등록하고, 이미 존재하는 경우 created를 증가시키지 않음
            if (webhookDeliveryPortOut.register(
                    project.getId(), deliveryId, eventName, payload.action(),
                    payload.value(), expiresAt
            ).created()) {
                created++;
            }
        }

        // WebhookDelivery가 이미 존재하는 경우, "duplicate" 상태를 반환하고, 새로 등록된 경우 "accepted" 상태를 반환
        String status = created == 0 ? "duplicate" : "accepted";
        return new WebhookReceiptResult(status, deliveryId, projects.size());
    }

    @Override
    public WebhookTestResult startTest(Long userId, Long projectId) {
        projectAccessResolver.resolveActive(userId, projectId);
        String testId = UUID.randomUUID().toString();
        WebhookDelivery delivery = webhookDeliveryPortOut.registerTest(
                projectId,
                testId,
                OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(TEST_TIMEOUT_SECONDS)
        ).delivery();
        return toTestResult(testId, delivery);
    }

    @Override
    public WebhookTestResult getTest(Long userId, Long projectId, String testId) {
        projectAccessResolver.resolveActive(userId, projectId);
        if (!StringUtils.hasText(testId)) {
            throw new CustomException(WebhookErrorCode.WEBHOOK_TEST_NOT_FOUND);
        }
        WebhookDelivery delivery = webhookDeliveryPortOut.getTest(projectId, testId)
                .orElseThrow(() -> new CustomException(WebhookErrorCode.WEBHOOK_TEST_NOT_FOUND));
        return toTestResult(testId, delivery);
    }

    private WebhookTestResult toTestResult(String testId, WebhookDelivery delivery) {
        String status = switch (delivery.status()) {
            case RECEIVED -> "queued";
            case PROCESSING -> "processing";
            case PROCESSED, IGNORED -> "succeeded";
            case FAILED -> "failed";
        };
        WebhookTestResult.Error error = delivery.errorCode() == null ? null
                : new WebhookTestResult.Error(
                        delivery.errorCode(), delivery.errorMessage(), true
                );
        boolean hasReceivedDelivery = StringUtils.hasText(delivery.action());
        return new WebhookTestResult(
                testId,
                status,
                delivery.action(),
                hasReceivedDelivery ? delivery.updatedAt() : null,
                delivery.processedAt(),
                error
        );
    }
}
