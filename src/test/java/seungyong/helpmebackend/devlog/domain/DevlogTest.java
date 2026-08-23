package seungyong.helpmebackend.devlog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevlogTest {
    @Test
    @DisplayName("작성하지 않은 날짜는 식별자와 version이 없는 빈 개발로그로 표현")
    void empty_success() {
        Devlog devlog = Devlog.empty(101L, LocalDate.of(2026, 8, 23));

        assertThat(devlog.exists()).isFalse();
        assertThat(devlog.id()).isNull();
        assertThat(devlog.contentMarkdown()).isEmpty();
        assertThat(devlog.version()).isNull();
    }

    @Test
    @DisplayName("저장된 개발로그는 version이 필수")
    void persistedDevlog_requiresVersion() {
        assertThatThrownBy(() -> new Devlog(
                301L,
                101L,
                LocalDate.of(2026, 8, 23),
                "내용",
                null,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
