package seungyong.helpmebackend.common.exception;

import lombok.Getter;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;

@Getter
public class GithubRateLimitException extends CustomException {
    private final int resetTime;

    public GithubRateLimitException(int resetTime) {
        super(RepositoryErrorCode.GITHUB_RATE_LIMIT_EXCEEDED);
        this.resetTime = resetTime;
    }
}
