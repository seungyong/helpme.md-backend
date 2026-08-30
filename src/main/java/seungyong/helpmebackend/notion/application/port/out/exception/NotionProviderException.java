package seungyong.helpmebackend.notion.application.port.out.exception;

import lombok.Getter;

@Getter
public class NotionProviderException extends RuntimeException {
    private final Failure failure;
    private final long retryAfterSeconds;

    public NotionProviderException(Failure failure) {
        this(failure, 0);
    }

    public NotionProviderException(Failure failure, long retryAfterSeconds) {
        super(failure.name());
        this.failure = failure;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public enum Failure {
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        BAD_REQUEST,
        RATE_LIMIT,
        UPSTREAM
    }
}
