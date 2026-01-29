package seungyong.helpmebackend.usecase.port.out.redis;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;

public interface RedisPortOut {
    void set(String key, String value, LocalDateTime expireTime);
    void setObject(String key, Object value, LocalDateTime expireTime);
    void setObjectIfAbsent(String key, Object value, LocalDateTime expireTime);
    boolean exists(String key);
    String get(String key);
    <T> T getObject(String key, TypeReference<T> typeRef);
    void delete(String key);
}
