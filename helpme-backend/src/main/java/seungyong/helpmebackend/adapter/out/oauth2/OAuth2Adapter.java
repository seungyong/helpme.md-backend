package seungyong.helpmebackend.adapter.out.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.installation.Installation;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.infrastructure.github.GithubAPI;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.oauth2.OAuth2PortOut;

import java.util.ArrayList;
import java.util.HashMap;
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
    public String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> body = new HashMap<>();
        body.put("client_id", super.getClientId());
        body.put("client_secret", super.getClientSecret());
        body.put("code", code);
        body.put("redirect_uri", super.getRedirectUri());

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = super.getRestTemplate().postForEntity(
                url,
                request,
                String.class
        );

        String responseBody = response.getBody();

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            log.error("Error parsing GitHub access token response = {}", responseBody);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public GithubUser getGithubUser(String accessToken) {
        String url = "https://api.github.com/user";
        String responseBody = githubAPI.fetchGetMethod(url, accessToken);

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
        String responseBody = githubAPI.fetchGetMethod(url, accessToken);

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
