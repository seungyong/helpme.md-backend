package seungyong.helpmebackend.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.", "USER_40401")
    ;

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
