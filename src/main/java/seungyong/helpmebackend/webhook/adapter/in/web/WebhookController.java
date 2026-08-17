package seungyong.helpmebackend.webhook.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.webhook.adapter.in.web.dto.response.ResponseStartedWebhookTest;
import seungyong.helpmebackend.webhook.adapter.in.web.dto.response.ResponseWebhookReceipt;
import seungyong.helpmebackend.webhook.adapter.in.web.dto.response.ResponseWebhookTest;
import seungyong.helpmebackend.webhook.application.port.in.WebhookPortIn;

@Tag(name = "Webhook", description = "GitHub Webhook 수신·테스트 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class WebhookController {
    private final WebhookPortIn webhookPortIn;

    @Operation(summary = "GitHub Webhook 수신")
    @PostMapping("/webhooks/github")
    public ResponseEntity<ResponseWebhookReceipt> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventName,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody byte[] rawBody
    ) {
        return ResponseEntity.accepted().body(ResponseWebhookReceipt.from(
                webhookPortIn.receive(signature, eventName, deliveryId, rawBody)
        ));
    }

    @Operation(summary = "Webhook 테스트 시작")
    @UserRoleApiErrors
    @PostMapping("/projects/{projectId}/webhook-tests")
    public ResponseEntity<ResponseStartedWebhookTest> startTest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId
    ) {
        ResponseStartedWebhookTest response = ResponseStartedWebhookTest.from(
                projectId,
                webhookPortIn.startTest(userDetails.getUserId(), projectId)
        );
        return ResponseEntity.accepted()
                .header(HttpHeaders.LOCATION, response.location())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(response.retryAfterSeconds()))
                .body(response);
    }

    @Operation(summary = "Webhook 테스트 상태 조회")
    @UserRoleApiErrors
    @GetMapping("/projects/{projectId}/webhook-tests/{testId}")
    public ResponseEntity<ResponseWebhookTest> getTest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable String testId
    ) {
        return ResponseEntity.ok(ResponseWebhookTest.from(
                webhookPortIn.getTest(userDetails.getUserId(), projectId, testId)
        ));
    }
}
