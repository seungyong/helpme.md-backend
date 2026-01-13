package seungyong.helpmebackend.adapter.out.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.infrastructure.redis.RedisStore;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RedisAdapter implements RedisPortOut {
    private final RedisStore redisStore;

    @Override
    public void save(String key, String value, LocalDateTime expireTime) {
        redisStore.set(key, value, expireTime);
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
    public void delete(String key) {
        redisStore.delete(key);
    }
}
