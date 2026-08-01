package seungyong.helpmebackend.global.infrastructure.github;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GithubRateLimitGuardTest {
    @Mock private RedisPortOut redisPortOut;

    @Test
    @DisplayName("사용자별 Redis key의 TTL을 올림하여 재시도 시간으로 반환한다")
    void getRetryAfterSeconds_usesUserTtl() {
        GithubRateLimitGuard guard = new GithubRateLimitGuard(redisPortOut);
        given(redisPortOut.getTimeToLive("github:rate-limit:1"))
                .willReturn(Optional.of(Duration.ofMillis(1_500)));

        assertThat(guard.getRetryAfterSeconds(1L)).hasValue(2);
    }

    @Test
    @DisplayName("rate limit 재시도 시간을 Redis TTL로 저장한다")
    void block_setsTtl() {
        GithubRateLimitGuard guard = new GithubRateLimitGuard(redisPortOut);
        given(redisPortOut.getTimeToLive("github:rate-limit:1")).willReturn(Optional.empty());

        guard.block(1L, 17);

        verify(redisPortOut).setWithTtl(
                "github:rate-limit:1",
                "blocked",
                Duration.ofSeconds(17)
        );
    }

    @Test
    @DisplayName("기존 TTL보다 짧은 rate limit 응답은 차단 시간을 줄이지 않는다")
    void block_doesNotShortenExistingTtl() {
        GithubRateLimitGuard guard = new GithubRateLimitGuard(redisPortOut);
        given(redisPortOut.getTimeToLive("github:rate-limit:1"))
                .willReturn(Optional.of(Duration.ofSeconds(30)));

        guard.block(1L, 10);

        verify(redisPortOut, never()).setWithTtl(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("Redis 조회 장애 시 GitHub 호출을 막지 않는다")
    void getRetryAfterSeconds_failsOpen() {
        GithubRateLimitGuard guard = new GithubRateLimitGuard(redisPortOut);
        given(redisPortOut.getTimeToLive("github:rate-limit:1"))
                .willThrow(new CustomException(GlobalErrorCode.REDIS_ERROR));

        assertThat(guard.getRetryAfterSeconds(1L)).isEmpty();
    }
}
