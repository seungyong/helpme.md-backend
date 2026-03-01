package seungyong.helpmebackend.common.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import seungyong.helpmebackend.adapter.in.web.util.CookieUtil;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final CookieUtil cookieUtil;

    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException throw Exception = {}", e.getMessage());
        log.error("Trace = {}", (Object) e.getStackTrace());
        return ErrorResponse.toResponseEntity(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = { CustomException.class })
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletResponse response) {
        log.warn("handleCustomException throw CustomException : {}", e.getErrorCode());

        if (e.getErrorCode().getErrorCode().equals(GlobalErrorCode.INVALID_TOKEN.getErrorCode())) {
            log.warn("Invalid token detected. Detailed trace: {}", (Object) e.getStackTrace());
            cookieUtil.clearTokenCookie(response);
        }

        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("handleHttpMessageNotReadable throw BadRequestException : {}", e.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(GlobalErrorCode.BAD_REQUEST.getErrorCode())
                .error(GlobalErrorCode.BAD_REQUEST.getHttpStatus().name())
                .code(GlobalErrorCode.BAD_REQUEST.getName())
                .message(GlobalErrorCode.BAD_REQUEST.getMessage())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("handleValidationException throw CustomException : {}", e.getStatusCode());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(status.value())
                        .errorCode(GlobalErrorCode.BAD_REQUEST.getErrorCode())
                        .error(GlobalErrorCode.BAD_REQUEST.getHttpStatus().name())
                        .code(GlobalErrorCode.BAD_REQUEST.getName())
                        .message(e.getBindingResult().getAllErrors().get(0).getDefaultMessage())
                        .build()
                );
    }
}
