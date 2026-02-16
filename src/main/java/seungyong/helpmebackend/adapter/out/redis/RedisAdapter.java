package seungyong.helpmebackend.adapter.out.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.infrastructure.redis.RedisStore;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RedisAdapter implements RedisPortOut {
    private final RedisStore redisStore;

    @Override
    public void set(String key, String value, Instant expireAt) {
        redisStore.set(key, value, expireAt);
    }

    @Override
    public void setObject(String key, Object value, Instant expireAt) {
        redisStore.setObject(key, value, expireAt);
    }

    @Override
    public void setObjectIfAbsent(String key, Object value, Instant expireAt) {
        redisStore.setObjectIfAbsent(key, value, expireAt);
    }


    @Override
    public boolean exists(String key) {
        return redisStore.exists(key);
    }

    @Override
    public String get(String key) {
        return redisStore.get(key);
    }

    @Override
    public <T> T getObject(String key, TypeReference<T> typeRef) {
        return redisStore.getObject(key, typeRef);
    }

    @Override
    public void delete(String key) {
        redisStore.delete(key);
    }
}
