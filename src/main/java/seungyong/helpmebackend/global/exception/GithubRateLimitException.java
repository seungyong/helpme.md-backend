package seungyong.helpmebackend.global.exception;

import lombok.Getter;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

@Getter
public class GithubRateLimitException extends CustomException {
    private final int retryAfterSeconds;

    public GithubRateLimitException(int retryAfterSeconds) {
        super(RepositoryErrorCode.GITHUB_RATE_LIMIT_EXCEEDED);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
