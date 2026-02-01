package seungyong.helpmebackend.usecase.port.out.redis;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;

public interface RedisPortOut {
    void set(String key, String value, Instant expireAt);
    void setObject(String key, Object value, Instant expireAt);
    void setObjectIfAbsent(String key, Object value, Instant expireAt);
    boolean exists(String key);
    String get(String key);
    <T> T getObject(String key, TypeReference<T> typeRef);
    void delete(String key);
}
