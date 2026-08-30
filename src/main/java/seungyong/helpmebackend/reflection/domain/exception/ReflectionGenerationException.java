package seungyong.helpmebackend.reflection.domain.exception;

import lombok.Getter;

@Getter
public class ReflectionGenerationException extends RuntimeException {
    private final String errorCode;
    private final boolean retryable;

    public ReflectionGenerationException(
            String errorCode, String message, boolean retryable, Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
}
