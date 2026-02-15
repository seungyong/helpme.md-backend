package seungyong.helpmebackend.usecase.port.out.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class GithubPortConfig {
    @Value("${oauth2.github.apps.client-id}")
    private String clientId;

    @Value("${oauth2.github.apps.client-secret}")
    private String clientSecret;

    @Value("${oauth2.github.apps.redirect-uri}")
    private String redirectUri;

    private final ObjectMapper objectMapper = new ObjectMapper();
}
