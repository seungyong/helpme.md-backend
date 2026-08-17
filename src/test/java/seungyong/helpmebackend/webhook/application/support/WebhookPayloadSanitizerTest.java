package seungyong.helpmebackend.webhook.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.webhook.domain.exception.WebhookErrorCode;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookPayloadSanitizerTest {
    private final WebhookPayloadSanitizer sanitizer = new WebhookPayloadSanitizer(
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void keepsOnlyRequiredPushFields() {
        byte[] body = """
                {
                  "repository":{"id":778899,"full_name":"octocat/demo","private":true},
                  "installation":{"id":9001},
                  "sender":{"login":"octocat","email":"secret@example.com"},
                  "token":"secret",
                  "ref":"refs/heads/main",
                  "commits":[{"id":"abc","message":"feat: webhook","timestamp":"2026-08-17T00:00:00Z","author":{"username":"octocat","email":"secret@example.com"},"patch":"private source"}]
                }
                """.getBytes(StandardCharsets.UTF_8);

        WebhookPayloadSanitizer.SanitizedPayload result = sanitizer.sanitize("push", body);

        assertThat(result.repositoryId()).isEqualTo(778899L);
        assertThat(result.installationId()).isEqualTo(9001L);
        assertThat(result.value().toString()).doesNotContain("secret@example.com", "private source", "token");
        assertThat(result.value().toString()).contains("feat: webhook");
    }

    @Test
    void rejectsPayloadWithoutCanonicalIdentifiers() {
        assertThatThrownBy(() -> sanitizer.sanitize(
                "push", "{\"repository\":{}}".getBytes(StandardCharsets.UTF_8)
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", WebhookErrorCode.INVALID_WEBHOOK_PAYLOAD
                );
    }
}
