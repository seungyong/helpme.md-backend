package seungyong.helpmebackend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "필수사항 또는 형식을 지키지 않은 잘못된 요청입니다.", "VALID_400"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다.", "AUTH_40101"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "AUTH_40102"),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 액세스 토큰입니다.", "AUTH_40103"),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 서버 에러입니다.", "SERVER_500"),
    GITHUB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub 서버 에러입니다.", "GITHUB_50001"),
    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 서버 에러입니다.", "REDIS_50002"),
    ;

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
