package seungyong.helpmebackend.infrastructure.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RedisKey {
    // refresh_token:{userId}
    REFRESH_KEY("refresh-token:"),

    // oauth2:state:{state}
    OAUTH2_STATE_KEY("oauth2:state:"),

    // sse:emitter:push:{userId}
    SSE_EMITTER_EVALUATION_DRAFT_KEY("sse:emitter:draft:"),
    SSE_EMITTER_GENERATION_KEY("sse:emitter:generation:"),

    // github-auth:{userId}:{owner}/{name}
    GITHUB_AUTH_KEY("github-auth:")
    ;

    private final String value;
}
