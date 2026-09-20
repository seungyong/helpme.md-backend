package seungyong.helpmebackend.global.application.deletion;

import java.time.Duration;

public final class DeletionRetryPolicy {
    public static final int FAST_ATTEMPTS = 5;
    private DeletionRetryPolicy() {}

    // 빠른 재시도 5회 이후에는 운영 알림과 함께 하루 간격 자동 정리 유지
    public static Duration delay(int attempts) {
        return attempts >= FAST_ATTEMPTS ? Duration.ofDays(1)
                : Duration.ofSeconds(60L << Math.max(0, attempts - 1));
    }
}
