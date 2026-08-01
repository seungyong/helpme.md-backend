package seungyong.helpmebackend.global.infrastructure.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.exception.CustomException;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import static seungyong.helpmebackend.global.domain.type.RedisKey.GITHUB_RATE_LIMIT_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
class GithubRateLimitGuard {
    private static final String BLOCKED_VALUE = "blocked";

    private final RedisPortOut redisPortOut;

    OptionalInt getRetryAfterSeconds(Long userId) {
        if (userId == null) {
            return OptionalInt.empty();
        }

        try {
            return redisPortOut.getTimeToLive(key(userId))
                    .map(GithubRateLimitGuard::toRetryAfterSeconds)
                    .map(OptionalInt::of)
                    .orElseGet(OptionalInt::empty);
        } catch (CustomException e) {
            log.warn(
                    "GitHub rate-limit guard lookup failed: exceptionType={}",
                    e.getClass().getSimpleName()
            );
            return OptionalInt.empty();
        }
    }

    void block(Long userId, int retryAfterSeconds) {
        if (userId == null) {
            return;
        }

        int normalizedRetryAfterSeconds = Math.max(1, retryAfterSeconds);
        String key = key(userId);

        try {
            Optional<Duration> currentTtl = redisPortOut.getTimeToLive(key);
            if (currentTtl.isPresent()
                    && currentTtl.get().compareTo(Duration.ofSeconds(normalizedRetryAfterSeconds)) >= 0) {
                return;
            }

            redisPortOut.setWithTtl(
                    key,
                    BLOCKED_VALUE,
                    Duration.ofSeconds(normalizedRetryAfterSeconds)
            );
        } catch (CustomException e) {
            log.warn(
                    "GitHub rate-limit guard update failed: exceptionType={}",
                    e.getClass().getSimpleName()
            );
        }
    }

    private static int toRetryAfterSeconds(Duration ttl) {
        long ttlMillis = ttl.toMillis();
        long seconds = ttlMillis / 1_000 + (ttlMillis % 1_000 == 0 ? 0 : 1);
        return (int) Math.min(Math.max(1, seconds), Integer.MAX_VALUE);
    }

    private String key(Long userId) {
        return GITHUB_RATE_LIMIT_KEY.getValue() + userId;
    }
}
