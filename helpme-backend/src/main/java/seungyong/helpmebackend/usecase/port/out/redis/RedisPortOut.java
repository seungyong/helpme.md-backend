package seungyong.helpmebackend.usecase.port.out.redis;

import java.time.LocalDateTime;

public interface RedisPortOut {
    void save(String key, String value, LocalDateTime expireTime);
    boolean exists(String key);
    String get(String key);
    void delete(String key);
}
