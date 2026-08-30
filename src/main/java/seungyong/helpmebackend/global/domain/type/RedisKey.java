package seungyong.helpmebackend.global.domain.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RedisKey {
    // refresh_token:{userId}
    REFRESH_KEY("refresh-token:"),

    // oauth2:state:{state}
    OAUTH2_STATE_KEY("oauth2:state:"),

    // notion:oauth:state:{state}
    NOTION_OAUTH_STATE_KEY("notion:oauth:state:"),

    // sse:emitter:push:{userId}
    SSE_EMITTER_EVALUATION_DRAFT_KEY("sse:emitter:draft:"),
    SSE_EMITTER_GENERATION_KEY("sse:emitter:generation:"),

    // github-auth:{userId}:{owner}/{name}
    GITHUB_AUTH_KEY("github-auth:"),

    // github:component-auth:{userId}:{owner}/{name}
    GITHUB_COMPONENT_AUTH_KEY("github:component-auth:"),

    // github:rate-limit:{userId}
    GITHUB_RATE_LIMIT_KEY("github:rate-limit:")
    ;

    private final String value;
}
