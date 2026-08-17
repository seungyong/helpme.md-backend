package seungyong.helpmebackend.webhook.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookReceiptResult;
import seungyong.helpmebackend.webhook.application.port.out.WebhookDeliveryPortOut;
import seungyong.helpmebackend.webhook.application.support.WebhookPayloadSanitizer;
import seungyong.helpmebackend.webhook.application.support.WebhookSignatureVerifier;
import seungyong.helpmebackend.webhook.domain.entity.WebhookDelivery;
import seungyong.helpmebackend.webhook.domain.exception.WebhookErrorCode;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {
    @Mock private WebhookSignatureVerifier signatureVerifier;
    @Mock private WebhookPayloadSanitizer payloadSanitizer;
    @Mock private WebhookDeliveryPortOut webhookDeliveryPortOut;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private ProjectAccessResolver projectAccessResolver;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(
                signatureVerifier, payloadSanitizer, webhookDeliveryPortOut,
                projectPortOut, projectAccessResolver
        );
    }

    @Test
    void verifiesBeforeParsingAndRegistersOneDeliveryPerProject() {
        byte[] rawBody = "{}".getBytes();
        WebhookPayloadSanitizer.SanitizedPayload payload =
                new WebhookPayloadSanitizer.SanitizedPayload(
                        778899L, 9001L, null, Map.of("repository", Map.of("id", 778899L))
                );
        Project project = Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/demo").build();
        WebhookDelivery delivery = delivery(1L, 101L, "delivery-1", WebhookDeliveryStatus.RECEIVED);
        given(payloadSanitizer.sanitize("push", rawBody)).willReturn(payload);
        given(projectPortOut.getActiveByGithubRepository(9001L, 778899L))
                .willReturn(List.of(project));
        given(webhookDeliveryPortOut.register(
                eq(101L), eq("delivery-1"), eq("push"), eq(null), any(), any()
        )).willReturn(new WebhookDeliveryPortOut.RegisterResult(delivery, true));

        WebhookReceiptResult result = webhookService.receive(
                "sha256=valid", "push", "delivery-1", rawBody
        );

        assertThat(result.status()).isEqualTo("accepted");
        assertThat(result.projectCount()).isEqualTo(1);
        InOrder order = inOrder(signatureVerifier, payloadSanitizer, webhookDeliveryPortOut);
        order.verify(signatureVerifier).verify("sha256=valid", rawBody);
        order.verify(payloadSanitizer).sanitize("push", rawBody);
        order.verify(webhookDeliveryPortOut).register(
                eq(101L), eq("delivery-1"), eq("push"), eq(null), any(), any()
        );
    }

    @Test
    void returnsDuplicateForRedelivery() {
        byte[] rawBody = "{}".getBytes();
        given(payloadSanitizer.sanitize("ping", rawBody)).willReturn(
                new WebhookPayloadSanitizer.SanitizedPayload(778899L, 9001L, null, Map.of())
        );
        Project project = Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/demo").build();
        given(projectPortOut.getActiveByGithubRepository(9001L, 778899L))
                .willReturn(List.of(project));
        WebhookDelivery delivery = delivery(1L, 101L, "delivery-1", WebhookDeliveryStatus.PROCESSED);
        given(webhookDeliveryPortOut.register(
                eq(101L), eq("delivery-1"), eq("ping"), eq(null), any(), any()
        )).willReturn(new WebhookDeliveryPortOut.RegisterResult(delivery, false));

        assertThat(webhookService.receive(
                "sha256=valid", "ping", "delivery-1", rawBody
        ).status()).isEqualTo("duplicate");
    }

    @Test
    void hidesMissingOrForeignWebhookTest() {
        Project project = Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/demo").build();
        given(projectAccessResolver.resolveActive(1L, 101L)).willReturn(project);
        given(webhookDeliveryPortOut.getTest(101L, "missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.getTest(1L, 101L, "missing"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", WebhookErrorCode.WEBHOOK_TEST_NOT_FOUND
                );
    }

    private WebhookDelivery delivery(
            Long id, Long projectId, String deliveryId, WebhookDeliveryStatus status
    ) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-17T00:00:00Z");
        return new WebhookDelivery(
                id, projectId, deliveryId, "ping", null, status, Map.of(),
                null, null, (short) 0, null, null,
                status == WebhookDeliveryStatus.PROCESSED ? now : null,
                null, null, now, now
        );
    }
}
