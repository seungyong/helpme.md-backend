package seungyong.helpmebackend.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.mapper.CustomTimeStamp;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStore {
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis에 key, value를 저장합니다. <br />
     * 만료 시간을 지정하여 저장합니다.
     *
     * @param key   저장할 key
     * @param value 저장할 value
     */
    public void set(String key, String value, LocalDateTime expireTime) {
        if (expireTime.isBefore(LocalDateTime.now())) {
            log.error("Don't set the past time to Redis. key = {}, expireTime = {}", key, expireTime);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }

        Duration duration = Duration.between(LocalDateTime.now(), expireTime);
        long ttlInSeconds = duration.getSeconds();

        redisTemplate.opsForValue().set(key, value, ttlInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Redis에 key가 존재하는지 확인합니다.
     *
     * @param key   확인할 key
     * @return key 존재 여부
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Redis에서 key를 기준으로 value를 가져옵니다.
     *
     * @param key   가져올 key
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Redis에 저장된 key를 삭제합니다.
     *
     * @param key 삭제할 key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
