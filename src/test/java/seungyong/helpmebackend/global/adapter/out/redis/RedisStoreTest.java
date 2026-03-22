package seungyong.helpmebackend.global.adapter.out.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.user.domain.entity.JWTUser;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RedisStoreTest {
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @InjectMocks private RedisStore redisStore;

    @Nested
    @DisplayName("Redis 저장")
    class SetTests {
        @Test
        @DisplayName("성공")
        void set_Success() {
            Mockito
                    .when(redisTemplate.opsForValue())
                    .thenReturn(valueOperations);

            Mockito
                    .doNothing()
                    .when(valueOperations)
                    .set(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));

            redisStore.set("testKey", "testValue", Instant.now().plusSeconds(60));

            Mockito.verify(redisTemplate.opsForValue(), Mockito.times(1))
                    .set(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
        }

        @Test
        @DisplayName("실패 - 과거 시간")
        void set_Fail_PastTime() {
            assertThatThrownBy(() -> redisStore.set("testKey", "testValue", Instant.now().minusSeconds(60)))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.REDIS_ERROR);
        }
    }

    @Nested
    @DisplayName("객체 Redis 저장")
    class SetObjectTests {
        @Test
        @DisplayName("성공")
        void setObject_Success() {
            Mockito
                    .when(redisTemplate.opsForValue())
                    .thenReturn(valueOperations);

            Mockito
                    .doNothing()
                    .when(valueOperations)
                    .set(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));

            redisStore.setObject("testKey", new JWTUser(1L, "test-name"), Instant.now().plusSeconds(60));

            Mockito.verify(redisTemplate.opsForValue(), Mockito.times(1))
                    .set(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
        }

        @Test
        @DisplayName("성공 - ifAbsent")
        void setObject_Success_IfAbsent() {
            Mockito
                    .when(redisTemplate.opsForValue())
                    .thenReturn(valueOperations);

            Mockito
                    .doNothing()
                    .when(valueOperations)
                    .set(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));

            redisStore.setObject("testKey", new JWTUser(1L, "test-name"), Instant.now().plusSeconds(60));

            Mockito.verify(redisTemplate.opsForValue(), Mockito.times(1))
                    .set(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
        }

        @Test
        @DisplayName("실패 - 과거 시간")
        void setObject_Fail_PastTime() {
            assertThatThrownBy(() -> redisStore.setObject("testKey", new JWTUser(1L, "test-name"), Instant.now().minusSeconds(60)))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.REDIS_ERROR);
        }

        @Test
        @DisplayName("실패 - ifAbsent 과거 시간")
        void setObject_Fail_IfAbsent_PastTime() {
            assertThatThrownBy(() -> redisStore.setObject("testKey", new JWTUser(1L, "test-name"), Instant.now().minusSeconds(60)))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.REDIS_ERROR);
        }
    }

    @Nested
    @DisplayName("존재 여부 확인")
    class ExistsTests {
        @Test
        @DisplayName("성공 - 존재")
        void exists_ExistingKey() {
            Mockito
                    .when(redisTemplate.hasKey("existingKey"))
                    .thenReturn(true);

            boolean result = redisStore.exists("existingKey");

            assertThat(result).isTrue();
            Mockito.verify(redisTemplate, Mockito.times(1)).hasKey("existingKey");
        }

        @Test
        @DisplayName("성공 - 미존재")
        void exists_NonExistingKey() {
            Mockito
                    .when(redisTemplate.hasKey("nonExistingKey"))
                    .thenReturn(false);

            boolean result = redisStore.exists("nonExistingKey");

            assertThat(result).isFalse();
            Mockito.verify(redisTemplate, Mockito.times(1)).hasKey("nonExistingKey");
        }
    }

    @Nested
    @DisplayName("조회")
    class GetTests {
        @Test
        @DisplayName("성공 - 존재")
        void get_ExistingKey() {
            Mockito
                    .when(redisTemplate.opsForValue())
                    .thenReturn(valueOperations);

            Mockito
                    .when(valueOperations.get("existingKey"))
                    .thenReturn("testValue");

            String result = redisStore.get("existingKey");

            assertThat(result).isEqualTo("testValue");
            Mockito.verify(redisTemplate.opsForValue(), Mockito.times(1)).get("existingKey");
        }

        @Test
        @DisplayName("성공 - 미존재")
        void get_NonExistingKey() {
            Mockito
                    .when(redisTemplate.opsForValue())
                    .thenReturn(valueOperations);

            Mockito
                    .when(valueOperations.get("nonExistingKey"))
                    .thenReturn(null);

            String result = redisStore.get("nonExistingKey");

            assertThat(result).isNull();
            Mockito.verify(redisTemplate.opsForValue(), Mockito.times(1)).get("nonExistingKey");
        }

        @Test
        @DisplayName("성공 - 객체 존재")
        void getObject_ExistingKey() {
            String key = "existingKey";
            Mockito
                    .when(redisTemplate.opsForValue())
                    .thenReturn(valueOperations);

            Mockito
                    .when(valueOperations.get(Mockito.eq(key)))
                    .thenReturn("{\"id\":1,\"username\":\"test-name\"}");

            JWTUser result = redisStore.getObject(key, new TypeReference<JWTUser>() {});

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("test-name");
            Mockito.verify(redisTemplate.opsForValue(), Mockito.times(1)).get("existingKey");
        }

        @Test
        @DisplayName("성공 - 객체 미존재")
        void getObject_NullValue() {
            String key = "nullValueKey";
            Mockito
                    .when(redisTemplate.opsForValue())
                    .thenReturn(valueOperations);

            Mockito
                    .when(valueOperations.get(Mockito.eq(key)))
                    .thenReturn(null);

            JWTUser result = redisStore.getObject(key, new TypeReference<JWTUser>() {});

            assertThat(result).isNull();
            Mockito.verify(redisTemplate.opsForValue(), Mockito.times(1)).get(key);
        }
    }

    @Test
    @DisplayName("삭제 - 성공")
    void delete_Success() {
        String key = "testKey";
        Mockito
                .when(redisTemplate.delete(key))
                .thenReturn(true);

        redisStore.delete(key);

        Mockito.verify(redisTemplate, Mockito.times(1)).delete(key);
    }
}
