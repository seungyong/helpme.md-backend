package seungyong.helpmebackend.project.domain.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReflectionWeekdayTest {
    @Test
    @DisplayName("API 문자열과 PostgreSQL DOW 값을 요일로 변환")
    void convertValues() {
        assertThat(ReflectionWeekday.fromApiValue("sunday"))
                .isEqualTo(ReflectionWeekday.SUNDAY);
        assertThat(ReflectionWeekday.fromDatabaseValue((short) 0))
                .isEqualTo(ReflectionWeekday.SUNDAY);
        assertThat(ReflectionWeekday.SATURDAY.getApiValue()).isEqualTo("saturday");
        assertThat(ReflectionWeekday.SATURDAY.getDatabaseValue()).isEqualTo((short) 6);
    }

    @Test
    @DisplayName("알 수 없는 API 또는 DB 값을 거부")
    void rejectUnknownValues() {
        assertThatThrownBy(() -> ReflectionWeekday.fromApiValue("funday"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReflectionWeekday.fromDatabaseValue((short) 7))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
