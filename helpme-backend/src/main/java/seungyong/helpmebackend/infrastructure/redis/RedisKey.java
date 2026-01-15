package seungyong.helpmebackend.infrastructure.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RedisKey {
    // refresh_token:{userId}
    REFRESH_KEY("refresh_token:"),

    // user:repositories:{userId}
    REPOSITORIES_KEY("user:repositories:")
    ;

    private final String value;
}
