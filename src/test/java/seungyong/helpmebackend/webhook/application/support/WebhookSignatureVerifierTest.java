package seungyong.helpmebackend.webhook.application.support;

import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.webhook.domain.exception.WebhookErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignatureVerifierTest {
    private static final String SECRET = "test-webhook-secret";
    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(SECRET);

    @Test
    void acceptsValidSignature() throws Exception {
        byte[] body = "{\"repository\":{\"id\":1}}".getBytes(StandardCharsets.UTF_8);

        assertThatCode(() -> verifier.verify(sign(body), body)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidSignature() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> verifier.verify("sha256=00", body))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", WebhookErrorCode.INVALID_GITHUB_SIGNATURE
                );
    }

    private String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
