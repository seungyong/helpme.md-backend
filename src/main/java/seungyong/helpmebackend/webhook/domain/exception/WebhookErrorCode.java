package seungyong.helpmebackend.webhook.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum WebhookErrorCode implements ErrorCode {
    INVALID_WEBHOOK_PAYLOAD(HttpStatus.BAD_REQUEST, "Webhook payload 형식이 올바르지 않습니다.", "WEBHOOK_40001"),
    INVALID_GITHUB_SIGNATURE(HttpStatus.UNAUTHORIZED, "GitHub Webhook 서명이 올바르지 않습니다.", "WEBHOOK_40101"),
    WEBHOOK_TEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Webhook 테스트를 찾을 수 없습니다.", "WEBHOOK_40401"),
    WEBHOOK_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Webhook 처리에 실패했습니다.", "WEBHOOK_50001"),
    WEBHOOK_TEST_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "제한 시간 안에 테스트 delivery를 받지 못했습니다.", "WEBHOOK_50401");

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
