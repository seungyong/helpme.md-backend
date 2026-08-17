package seungyong.helpmebackend.webhook.application.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.webhook.domain.exception.WebhookErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {
    private static final String PREFIX = "sha256=";
    private final byte[] secret;

    public WebhookSignatureVerifier(
            @Value("${oauth2.github.apps.webhook-secret:}") String webhookSecret
    ) {
        this.secret = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String signature, byte[] body) {
        if (!StringUtils.hasText(signature) || !signature.startsWith(PREFIX)
                || secret.length == 0 || body == null) {
            throw new CustomException(WebhookErrorCode.INVALID_GITHUB_SIGNATURE);
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));

            // 응답 body를 HMAC-SHA256으로 해싱하고, signature와 비교
            byte[] expected = mac.doFinal(body);
            // signature에서 "sha256=" 접두사를 제거하고, hex 문자열을 바이트 배열로 변환
            byte[] supplied = HexFormat.of().parseHex(signature.substring(PREFIX.length()));

            // 타이밍 공격을 방지하기 위해 MessageDigest.isEqual()을 사용하여 비교
            // 타이밍 공격 : 공격자가 해시 비교 시 걸리는 시간을 측정하여, 올바른 해시 값을 추측하는 공격 기법
            if (!MessageDigest.isEqual(expected, supplied)) {
                throw new CustomException(WebhookErrorCode.INVALID_GITHUB_SIGNATURE);
            }
        } catch (IllegalArgumentException exception) {
            throw new CustomException(WebhookErrorCode.INVALID_GITHUB_SIGNATURE);
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Webhook signature verifier is unavailable", exception);
        }
    }
}
