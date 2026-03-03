package seungyong.helpmebackend.auth.application.port.out.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuthTokenResult(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        String scope
) {
}
