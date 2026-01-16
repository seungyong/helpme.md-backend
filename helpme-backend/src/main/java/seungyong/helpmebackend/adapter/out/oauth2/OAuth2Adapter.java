package seungyong.helpmebackend.adapter.out.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.result.OAuthTokenResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.installation.Installation;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.infrastructure.github.GithubAPI;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.oauth2.OAuth2PortOut;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2Adapter extends GithubPortConfig implements OAuth2PortOut {
    private final GithubAPI githubAPI;

    @Override
    public String generateLoginUrl(String state) {
        return "https://github.com/login/oauth/authorize?client_id=" + super.getClientId()
                + "&scope=read:user"
                + "&state=" + state;
    }

    @Override
    public OAuthTokenResult getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "client_id", super.getClientId(),
                "client_secret", super.getClientSecret(),
                "code", code,
                "redirect_uri", super.getRedirectUri()
        );

        OAuthTokenResult response = githubAPI.postNoAuth(url, headers, body, OAuthTokenResult.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            log.error("Error fetching GitHub access token with code = {}", code);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }

        return response;
    }

    @Override
    public GithubUser getGithubUser(String accessToken) {
        String url = "https://api.github.com/user";
        String responseBody = githubAPI.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
            return new GithubUser(
                    jsonNode.get("login").asText(),
                    jsonNode.get("id").asLong(),
                    accessToken
            );
        } catch (Exception e) {
            log.error("Error parsing GitHub user response = {}", responseBody);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public ArrayList<Installation> getInstallations(String accessToken) {
        String url = "https://api.github.com/user/installations?per_page=100";
        String responseBody = githubAPI.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
            ArrayList<Installation> installations = new ArrayList<>();

            for (JsonNode item : jsonNode.get("installations")) {
                installations.add(new Installation(
                        item.get("id").asText(),
                        item.get("account").get("avatar_url").asText(),
                        item.get("account").get("login").asText()
                ));
            }

            return installations;
        } catch (Exception e) {
            log.error("Error parsing GitHub installations response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }
}
