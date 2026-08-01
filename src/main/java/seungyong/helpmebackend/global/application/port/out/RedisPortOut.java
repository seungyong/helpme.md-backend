package seungyong.helpmebackend.global.application.port.out;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface RedisPortOut {
    void set(String key, String value, Instant expireAt);
    void setWithTtl(String key, String value, Duration ttl);
    void setObject(String key, Object value, Instant expireAt);
    void setObjectIfAbsent(String key, Object value, Instant expireAt);
    boolean exists(String key);
    Optional<Duration> getTimeToLive(String key);
    String get(String key);
    <T> T getObject(String key, TypeReference<T> typeRef);
    void delete(String key);
}
