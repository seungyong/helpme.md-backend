package seungyong.helpmebackend.auth.adapter.out.github;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.auth.domain.entity.Installation;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.global.config.GithubPortConfig;
import seungyong.helpmebackend.auth.application.port.out.OAuth2PortOut;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2Adapter extends GithubPortConfig implements OAuth2PortOut {
    private final GithubApiExecutor githubApiExecutor;

    @Override
    public String generateLoginUrl(String state) {
        return "https://github.com/login/oauth/authorize?client_id=" + super.getClientId()
                + "&scope=read:user"
                + "&redirect_uri=" + super.getRedirectUri()
                + "&state=" + state;
    }

    @Override
    public OAuthTokenResult getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        return githubApiExecutor.executePostNoAuth(
                url,
                Map.of(
                        "client_id", super.getClientId(),
                        "client_secret", super.getClientSecret(),
                        "code", code,
                        "redirect_uri", super.getRedirectUri()
                ),
                OAuthTokenResult.class,
                "Get GitHub Access Token with code = " + code
        );
    }

    @Override
    public GithubUser getGithubUser(String accessToken) {
        String url = "https://api.github.com/user";

        return githubApiExecutor.executeGet(
                url,
                accessToken,
                jsonNode -> new GithubUser(
                        jsonNode.get("login").asText(),
                        jsonNode.get("id").asLong(),
                        accessToken
                ),
                "Get GitHub User with accessToken"
        );
    }

    @Override
    public List<Installation> getInstallations(String accessToken) {
        String url = "https://api.github.com/user/installations?per_page=100";

        return githubApiExecutor.executeGet(
                url,
                accessToken,
                jsonNode -> {
                    ArrayList<Installation> installations = new ArrayList<>();

                    for (JsonNode item : jsonNode.get("installations")) {
                        installations.add(new Installation(
                                item.get("id").asText(),
                                item.get("account").get("avatar_url").asText(),
                                item.get("account").get("login").asText()
                        ));
                    }

                    return installations;
                },
                "Get GitHub Installations with accessToken"
        );
    }
}
