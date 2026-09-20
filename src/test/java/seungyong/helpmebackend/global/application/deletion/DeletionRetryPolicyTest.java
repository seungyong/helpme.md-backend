package seungyong.helpmebackend.global.application.deletion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class DeletionRetryPolicyTest {
    @Test
    @DisplayName("삭제 실패 후 1·2·4·8분 backoff, 5번째부터 하루 간격으로 재시도")
    void boundsFastRetriesAndKeepsDailyCleanup() {
        assertThat(DeletionRetryPolicy.delay(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(DeletionRetryPolicy.delay(2)).isEqualTo(Duration.ofMinutes(2));
        assertThat(DeletionRetryPolicy.delay(3)).isEqualTo(Duration.ofMinutes(4));
        assertThat(DeletionRetryPolicy.delay(4)).isEqualTo(Duration.ofMinutes(8));
        assertThat(DeletionRetryPolicy.delay(5)).isEqualTo(Duration.ofDays(1));
        assertThat(DeletionRetryPolicy.delay(100)).isEqualTo(Duration.ofDays(1));
    }
}
