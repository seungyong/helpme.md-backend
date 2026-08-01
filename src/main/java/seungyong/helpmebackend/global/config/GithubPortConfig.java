package seungyong.helpmebackend.global.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class GithubPortConfig {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GithubPortConfig(
            @Value("${oauth2.github.apps.client-id}") String clientId,
            @Value("${oauth2.github.apps.client-secret}") String clientSecret,
            @Value("${oauth2.github.apps.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }
}
