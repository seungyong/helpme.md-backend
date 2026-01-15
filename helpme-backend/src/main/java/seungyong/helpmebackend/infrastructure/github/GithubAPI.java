package seungyong.helpmebackend.infrastructure.github;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubAPI {
    public String fetchGetMethod(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = new org.springframework.web.client.RestTemplate().exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        return response.getBody();
    }
}
