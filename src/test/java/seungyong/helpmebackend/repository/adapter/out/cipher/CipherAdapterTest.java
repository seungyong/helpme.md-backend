package seungyong.helpmebackend.repository.adapter.out.cipher;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CipherAdapter.class)
class CipherAdapterTest {
    @Autowired private CipherAdapter cipherAdapter;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Nested
    @DisplayName("encrypt - 암호화")
    class Encrypt {
        @Test
        @DisplayName("성공")
        void encrypt_success() {
            String plainText = fixtureMonkey.giveMeOne(String.class);

            String encrypted = cipherAdapter.encrypt(plainText);

            assertThat(encrypted).isNotEqualTo(plainText);
            assertThat(encrypted).isBase64();
        }
    }

    @Nested
    @DisplayName("decrypt - 복호화")
    class Decrypt {

        @Test
        @DisplayName("성공")
        void decrypt_success() {
            String plainText = "test-message";
            String encrypted = cipherAdapter.encrypt(plainText);

            String decrypted = cipherAdapter.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("성공 - 빈 문자열이나 null인 경우")
        void decrypt_success_null_or_blank() {
            assertThat(cipherAdapter.decrypt(null)).isNull();
            assertThat(cipherAdapter.decrypt("")).isNull();
        }

        @Test
        @DisplayName("실패 - 잘못된 형식의 암호문인 경우")
        void decrypt_failure_invalid_format() {
            String invalidCipherText = "invalid-base64-format";

            assertThatThrownBy(() -> cipherAdapter.decrypt(invalidCipherText))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}