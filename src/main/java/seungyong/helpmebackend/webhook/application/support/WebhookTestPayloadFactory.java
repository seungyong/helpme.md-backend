package seungyong.helpmebackend.webhook.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.project.domain.entity.Project;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
public class WebhookTestPayloadFactory {
    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public WebhookTestPayloadFactory(
            @Value("${oauth2.github.apps.webhook-secret:}") String webhookSecret,
            ObjectMapper objectMapper
    ) {
        this.secret = webhookSecret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    public SignedPayload create(Project project) {
        if (secret.length == 0) {
            throw new IllegalStateException("GitHub Webhook secret is not configured");
        }
        try {
            String deliveryId = UUID.randomUUID().toString();
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "hook_id", 0,
                    "zen", "Helpme.md webhook test",
                    "repository", Map.of(
                            "id", project.getGithubRepoId(),
                            "full_name", project.getRepoFullName(),
                            "private", project.isPrivateRepository()
                    ),
                    "installation", Map.of("id", project.getGithubInstallationId()),
                    "sender", Map.of("login", "helpme-md")
            ));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String signature = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
            return new SignedPayload(deliveryId, signature, body);
        } catch (Exception exception) {
            throw new IllegalStateException("Webhook test payload could not be created", exception);
        }
    }

    public record SignedPayload(String deliveryId, String signature, byte[] body) {
        public SignedPayload {
            body = body.clone();
        }

        @Override
        public byte[] body() {
            return body.clone();
        }
    }
}
