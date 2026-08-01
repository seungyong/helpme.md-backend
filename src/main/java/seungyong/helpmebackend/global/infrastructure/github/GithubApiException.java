package seungyong.helpmebackend.global.infrastructure.github;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class GithubApiException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final boolean rateLimited;
    private final Integer retryAfterSeconds;

    public GithubApiException(
            HttpStatusCode statusCode,
            boolean rateLimited,
            Integer retryAfterSeconds,
            Throwable cause
    ) {
        super(cause);
        this.statusCode = statusCode;
        this.rateLimited = rateLimited;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean hasStatus(HttpStatus status) {
        return statusCode != null && statusCode.value() == status.value();
    }
}
