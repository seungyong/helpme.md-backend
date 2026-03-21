package seungyong.helpmebackend.repository.adapter.out.cipher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ObjectCipherDecoratorTest {
    @Mock private CipherPortOut cipherPortOut;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private ObjectCipherDecorator objectCipherDecorator;

    @Nested
    @DisplayName("encrypt - 객체 암호화")
    class Encrypt {
        @Test
        @DisplayName("성공")
        void encrypt_success() {
            Map<String, String> data = Map.of("key", "value");
            String encryptedString = "encrypted-string";
            given(cipherPortOut.encrypt(anyString())).willReturn(encryptedString);

            String result = objectCipherDecorator.encrypt(data);

            assertThat(result).isEqualTo(encryptedString);
        }

        @Test
        @DisplayName("성공 - 데이터가 null인 경우")
        void encrypt_success_null() {
            String result = objectCipherDecorator.encrypt(null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("실패 - 직렬화 또는 암호화 중 예외 발생")
        void encrypt_failure_exception() {
            Map<String, String> data = Map.of("key", "value");
            given(cipherPortOut.encrypt(any())).willThrow(new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> objectCipherDecorator.encrypt(data))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("decrypt - 객체 복호화")
    class Decrypt {
        @Test
        @DisplayName("성공")
        void decrypt_success() throws Exception {
            String encryptedText = "encrypted-text";
            Map<String, String> expectedData = Map.of("key", "value");
            String json = objectMapper.writeValueAsString(expectedData);

            given(cipherPortOut.decrypt(encryptedText)).willReturn(json);

            Map<String, String> result = objectCipherDecorator.decrypt(
                    encryptedText,
                    new TypeReference<Map<String, String>>() {}
            );

            assertThat(result).isEqualTo(expectedData);
        }

        @Test
        @DisplayName("성공 - 암호문이 null인 경우")
        void decrypt_success_null() {
            Map<String, String> result = objectCipherDecorator.decrypt(
                    null,
                    new TypeReference<Map<String, String>>() {}
            );

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("실패 - 잘못된 형식의 암호문인 경우")
        void decrypt_failure_invalid_format() {
            String encryptedText = "invalid-text";
            given(cipherPortOut.decrypt(anyString())).willThrow(new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> objectCipherDecorator.decrypt(encryptedText, new TypeReference<Object>() {}))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}