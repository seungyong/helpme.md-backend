package seungyong.helpmebackend.adapter.out.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.result.OAuthTokenResult;
import seungyong.helpmebackend.domain.entity.installation.Installation;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.oauth2.OAuth2PortOut;

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
