package seungyong.helpmebackend.global.exception;

import lombok.Getter;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

@Getter
public class GithubRateLimitException extends CustomException {
    private final int retryAfterSeconds;

    public GithubRateLimitException(int retryAfterSeconds) {
        this(RepositoryErrorCode.GITHUB_RATE_LIMIT_EXCEEDED, retryAfterSeconds);
    }

    public GithubRateLimitException(ErrorCode errorCode, int retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
