package seungyong.helpmebackend.global.adapter.out.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisAdapter implements RedisPortOut {
    private final RedisStore redisStore;

    @Override
    public void set(String key, String value, Instant expireAt) {
        redisStore.set(key, value, expireAt);
    }

    @Override
    public void setWithTtl(String key, String value, Duration ttl) {
        redisStore.setWithTtl(key, value, ttl);
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
    public Optional<Duration> getTimeToLive(String key) {
        return redisStore.getTimeToLive(key);
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
    public <T> T getObjectAndDelete(String key, TypeReference<T> typeRef) {
        return redisStore.getObjectAndDelete(key, typeRef);
    }

    @Override
    public void delete(String key) {
        redisStore.delete(key);
    }
}
