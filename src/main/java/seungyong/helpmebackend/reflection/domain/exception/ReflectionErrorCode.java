package seungyong.helpmebackend.reflection.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ReflectionErrorCode implements ErrorCode {
    REFLECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다.", "REFLECTION_40401"),
    REFLECTION_SOURCE_INSUFFICIENT(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "회고를 생성할 근거가 부족합니다.",
            "REFLECTION_42201"
    ),
    REFLECTION_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "회고 초안 생성에 실패했습니다.",
            "REFLECTION_50001"
    ),
    REFLECTION_RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "회고 생성 요청 한도를 초과했습니다.",
            "RATE_42902"
    );

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
