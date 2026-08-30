package seungyong.helpmebackend.global.adapter.out.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
class RedisStore {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Redis에 key, value를 저장합니다. <br />
     * 만료 시간을 지정하여 저장합니다.
     *
     * @param key   저장할 key
     * @param value 저장할 value
     * @param expireAt 만료 시간
     */
    public void set(String key, String value, Instant expireAt) {
        if (expireAt.isBefore(Instant.now())) {
            log.error("Don't set the past time to Redis. key = {}, expireTime = {}", key, expireAt);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }

        try {
            Duration duration = Duration.between(Instant.now(), expireAt);
            long ttlInSeconds = duration.getSeconds();

            redisTemplate.opsForValue().set(key, value, ttlInSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis set error. key = {}, value = {}, expireTime = {}", key, value, expireAt, e);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }
    }

    /**
     * Redis에 key, value 객체를 저장합니다. <br />
     * 만료 시간을 지정하여 저장합니다.
     *
     * @param key   저장할 key
     * @param value 저장할 value 객체
     * @param expireAt 만료 시간
     */
    public void setObject(String key, Object value, Instant expireAt) {
        if (expireAt.isBefore(Instant.now())) {
            log.error("Don't set the past time to Redis. key = {}, expireTime = {}", key, expireAt);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }

        try {
            Duration duration = Duration.between(Instant.now(), expireAt);
            long ttlInSeconds = duration.getSeconds();

            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttlInSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis setObject error. key = {}, value = {}, expireTime = {}", key, value, expireAt, e);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }
    }

    /**
     * Redis에 key가 없을 경우에만 key, value 객체를 저장합니다. <br />
     * 비동기와 같이 중복 저장을 방지할 때 사용합니다. <br />
     * 만료 시간을 지정하여 저장합니다.
     *
     * @param key   저장할 key
     * @param value 저장할 value 객체
     * @param expireAt 만료 시간
     */
    public void setObjectIfAbsent(String key, Object value, Instant expireAt) {
        if (expireAt.isBefore(Instant.now())) {
            log.error("Don't set the past time to Redis. key = {}, expireTime = {}", key, expireAt);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }

        try {
            Duration duration = Duration.between(Instant.now(), expireAt);
            long ttlInSeconds = duration.getSeconds();

            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    objectMapper.writeValueAsString(value),
                    ttlInSeconds,
                    TimeUnit.SECONDS
            );

            if (success == null || !success) {
                log.info("Redis setObjectIfAbsent skipped. key = {} already exists.", key);
            }
        } catch (Exception e) {
            log.error("Redis setObjectIfAbsent error. key = {}, value = {}, expireTime = {}", key, value, expireAt, e);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }
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
     * Redis key의 남은 TTL을 조회합니다.
     *
     * @param key 확인할 key
     * @return 만료 시간이 설정된 key의 남은 TTL, key가 없거나 만료 시간이 없으면 empty
     */
    public Optional<Duration> getTimeToLive(String key) {
        try {
            Long ttlMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            if (ttlMillis == null || ttlMillis <= 0) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofMillis(ttlMillis));
        } catch (Exception e) {
            log.error("Redis TTL lookup error. key = {}", key, e);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }
    }

    /**
     * Redis에 key, value를 지정한 TTL 동안 저장합니다.
     *
     * @param key 저장할 key
     * @param value 저장할 value
     * @param ttl 만료까지 남은 시간
     */
    public void setWithTtl(String key, String value, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            log.error("Don't set a non-positive TTL to Redis. key = {}, ttl = {}", key, ttl);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }

        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.error("Redis setWithTtl error. key = {}, ttl = {}", key, ttl, e);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }
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
     * Redis에서 key를 기준으로 value를 객체로 가져옵니다.
     *
     * @param key   가져올 key
     * @param <T>   변환할 객체 타입
     * @return 변환된 객체
     */
    public <T> T getObject(String key, TypeReference<T> typeRef) {
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, typeRef);
        } catch (Exception e) {
            log.error("Redis getObject error. key = {}", key, e);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }
    }

    /**
     * Redis 값을 원자적으로 가져오고 삭제합니다.
     * OAuth state처럼 한 번만 소비해야 하는 값에 사용합니다.
     */
    public <T> T getObjectAndDelete(String key, TypeReference<T> typeRef) {
        try {
            String value = redisTemplate.opsForValue().getAndDelete(key);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, typeRef);
        } catch (Exception e) {
            log.error("Redis getObjectAndDelete error. key = {}", key, e);
            throw new CustomException(GlobalErrorCode.REDIS_ERROR);
        }
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
