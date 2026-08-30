package seungyong.helpmebackend.notion.domain.exception;

import lombok.Getter;
import seungyong.helpmebackend.global.exception.CustomException;

@Getter
public class NotionRateLimitException extends CustomException {
    private final long retryAfterSeconds;

    public NotionRateLimitException(long retryAfterSeconds) {
        super(NotionErrorCode.NOTION_RATE_LIMIT_EXCEEDED);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
