package seungyong.helpmebackend.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getName();
    HttpStatus getHttpStatus();
    String getMessage();
    String getErrorCode();
}
