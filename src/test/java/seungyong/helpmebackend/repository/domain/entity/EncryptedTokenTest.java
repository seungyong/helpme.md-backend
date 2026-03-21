package seungyong.helpmebackend.repository.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EncryptedTokenTest {
    @Nested
    @DisplayName("EncryptedToken 생성자")
    class Constructor {
        @Test
        @DisplayName("성공")
        void generate_success() {
            String tokenValue = "encryptedToken123";

            EncryptedToken encryptedToken = new EncryptedToken(tokenValue);

            assertThat(encryptedToken.value()).isEqualTo(tokenValue);
        }

        @Test
        @DisplayName("실패 - null 값")
        void generate_fail_null() {
            String tokenValue = null;

            assertThatThrownBy(() -> new EncryptedToken(tokenValue))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("토큰 값은 null이거나 공백일 수 없습니다.");
        }

        @Test
        @DisplayName("실패 - 공백 값")
        void generate_fail_blank() {
            String tokenValue = "   ";

            assertThatThrownBy(() -> new EncryptedToken(tokenValue))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("토큰 값은 null이거나 공백일 수 없습니다.");
        }
    }
}
