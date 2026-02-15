package seungyong.helpmebackend.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum SseErrorCode implements ErrorCode {
    CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SSE 연결에 실패했습니다.", "SSE_50001"),
    ;

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
