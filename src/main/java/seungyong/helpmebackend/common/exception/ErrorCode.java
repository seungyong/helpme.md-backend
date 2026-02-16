package seungyong.helpmebackend.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getName();
    HttpStatus getHttpStatus();
    String getMessage();
    String getErrorCode();
}
