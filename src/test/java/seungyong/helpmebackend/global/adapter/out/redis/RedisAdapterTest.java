package seungyong.helpmebackend.global.adapter.out.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RedisAdapterTest {
    @Mock private RedisStore redisStore;
    @InjectMocks private RedisAdapter redisAdapter;

    @Nested
    @DisplayName("Redis 저장")
    class SetTests {
        @Test
        @DisplayName("set - 성공")
        void set_Success() {
            Mockito.doNothing().when(redisStore).set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Instant.class));

            redisAdapter.set("testKey", "testValue", Instant.now().plusSeconds(60));

            Mockito.verify(redisStore, Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Instant.class));
        }

        @Test
        @DisplayName("setObject - 성공")
        void setObject_Success() {
            Mockito.doNothing().when(redisStore).setObject(Mockito.anyString(), Mockito.any(), Mockito.any(Instant.class));

            redisAdapter.setObject("testKey", new Object(), Instant.now().plusSeconds(60));

            Mockito.verify(redisStore, Mockito.times(1)).setObject(Mockito.anyString(), Mockito.any(), Mockito.any(Instant.class));
        }

        @Test
        @DisplayName("setObjectIfAbsent - 성공")
        void setObjectIfAbsent_Success() {
            Mockito.doNothing().when(redisStore).setObjectIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.any(Instant.class));

            redisAdapter.setObjectIfAbsent("testKey", new Object(), Instant.now().plusSeconds(60));

            Mockito.verify(redisStore, Mockito.times(1)).setObjectIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.any(Instant.class));
        }
    }

    @Nested
    @DisplayName("Redis 존재 확인")
    class ExistsTests {
        @Test
        @DisplayName("성공 - true")
        void exists_Success() {
            Mockito.when(redisStore.exists(Mockito.anyString())).thenReturn(true);

            boolean result = redisAdapter.exists("testKey");

            assertThat(result).isTrue();
            Mockito.verify(redisStore, Mockito.times(1)).exists(Mockito.anyString());
        }

        @Test
        @DisplayName("성공 - false")
        void exists_False() {
            Mockito.when(redisStore.exists(Mockito.anyString())).thenReturn(false);

            boolean result = redisAdapter.exists("testKey");

            assertThat(result).isFalse();
            Mockito.verify(redisStore, Mockito.times(1)).exists(Mockito.anyString());
        }
    }

    @Nested
    @DisplayName("Redis 조회")
    class GetTests {
        @Test
        @DisplayName("get - 성공")
        void get_Success() {
            Mockito.when(redisStore.get(Mockito.anyString())).thenReturn("testValue");

            String result = redisAdapter.get("testKey");

            assertThat(result).isEqualTo("testValue");
            Mockito.verify(redisStore, Mockito.times(1)).get(Mockito.anyString());
        }

        @Test
        @DisplayName("getObject - 성공")
        void getObject_Success() {
            Object expectedObject = new Object();
            Mockito.when(redisStore.getObject(Mockito.anyString(), Mockito.any())).thenReturn(expectedObject);

            Object result = redisAdapter.getObject("testKey", null);

            assertThat(result).isEqualTo(expectedObject);
            Mockito.verify(redisStore, Mockito.times(1)).getObject(Mockito.anyString(), Mockito.any());
        }
    }

    @Test
    @DisplayName("Redis 삭제 - 성공")
    void delete_Success() {
        Mockito.doNothing().when(redisStore).delete(Mockito.anyString());

        redisAdapter.delete("testKey");

        Mockito.verify(redisStore, Mockito.times(1)).delete(Mockito.anyString());
    }
}
