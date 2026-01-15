package seungyong.helpmebackend.adapter.out.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortOut;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GithubAdapter implements GithubPortOut {
    @Value("${oauth2.github.apps.client_id}")
    private String clientId;

    @Value("${oauth2.github.apps.client_secret}")
    private String clientSecret;

    @Value("${oauth2.github.apps.redirect_uri}")
    private String redirectUri;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                request,
                String.class
        );

        String responseBody = response.getBody();

        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            log.error("Error parsing GitHub access token response = {}", responseBody);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public GithubUser getGithubUser(String accessToken) {
        String url = "https://api.github.com/user";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );
        String responseBody = response.getBody();

        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
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
}
