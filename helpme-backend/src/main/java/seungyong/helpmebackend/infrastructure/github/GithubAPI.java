package seungyong.helpmebackend.infrastructure.github;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class GithubAPI {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_VERSION = "2022-11-28";

    public static class Accept {
        public static final String APPLICATION_GITHUB_VND_GITHUB_JSON =
                "application/vnd.github+json";
        public static final String APPLICATION_GITHUB_VND_GITHUB_RAW_JSON =
                "application/vnd.github.raw+json";
    }

    /**
     * GitHub API 응답 헤더에서 'next' 페이지의 URL을 추출합니다.
     *
     * @param headers   GitHub API 응답 헤더
     * @return          'next' 페이지의 URL이 존재하면 Optional에 담아 반환, 없으면 빈 Optional 반환
     */
    public static Optional<String> extractNextUrl(HttpHeaders headers) {
        String link = headers.getFirst(HttpHeaders.LINK);
        if (link.isBlank()) { return Optional.empty(); }

        String[] parts = link.split(",\\s*");

        for (String part : parts) {
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<');
                int end = part.indexOf('>');

                if (start >= 0 && end > start) {
                    return Optional.of(part.substring(start + 1, end));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * GitHub API에 GET 요청을 보내고 응답을 지정된 타입으로 반환합니다.
     * - Authorization 헤더에 Bearer 토큰을 포함합니다. <br>
     * - Accept 헤더에 GitHub API 버전을 지정합니다. <br>
     * - Accept 헤더에 전달된 accept 값을 설정합니다. <br>
     * - X-GitHub-Api-Version 헤더에 API 버전을 설정합니다. <br>
     *
     * @param url           요청을 보낼 GitHub API의 URL
     * @param token         인증에 사용할 Bearer 토큰
     * @param responseType  응답 본문의 타입 클래스
     * @return              GitHub API의 응답 ResponseEntity
     * @param <T>           응답 본문의 타입
     */
    public <T> ResponseEntity<T> fetchGet(
            String url,
            String token,
            String accept,
            Class<T> responseType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.ACCEPT, accept);
        headers.set("X-GitHub-Api-Version", API_VERSION);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                responseType
        );
    }
    
    /**
     * GitHub API에 GET 요청을 보내고 응답 본문을 문자열로 반환합니다.
     * - Authorization 헤더에 Bearer 토큰을 포함합니다. <br>
     * - Accept 헤더에 GitHub API 버전을 지정합니다. <br>
     * - X-GitHub-Api-Version 헤더에 API 버전을 설정합니다. <br>
     *
     * @param url       요청을 보낼 GitHub API의 URL
     * @param token     인증에 사용할 Bearer 토큰
     * @return          GitHub API의 응답 본문 Body 문자열
     */
    public String fetchGetMethodForBody(String url, String token) {
        ResponseEntity<String> response = fetchGet(url, token, Accept.APPLICATION_GITHUB_VND_GITHUB_JSON, String.class);
        return response.getBody();
    }

    /**
     * GitHub API에 GET 요청을 보내고 응답 본문을 문자열로 반환합니다.
     * - Authorization 헤더에 Bearer 토큰을 포함합니다. <br>
     * - Accept 헤더에 GitHub API 버전을 지정합니다. <br>
     * - Accept 헤더에 전달된 accept 값을 설정합니다. <br>
     * - X-GitHub-Api-Version 헤더에 API 버전을 설정합니다. <br>
     *
     * @param url       요청을 보낼 GitHub API의 URL
     * @param token     인증에 사용할 Bearer 토큰
     * @return          GitHub API의 응답 본문 Body 문자열
     */
    public String fetchGetMethodForBody(String url, String token, String accept) {
        ResponseEntity<String> response = fetchGet(url, token, accept, String.class);
        return response.getBody();
    }

    /**
     * GitHub API에 POST 요청을 보내고 응답을 지정된 타입으로 반환합니다.
     * @param url           요청을 보낼 GitHub API의 URL
     * @param token         인증에 사용할 Bearer 토큰
     * @param body          요청 본문
     * @param responseType  응답 본문의 타입 클래스
     * @return              GitHub API의 응답 본문
     * @param <T>           응답 본문의 타입
     */
    public <T> T postWithBearer(String url, String token, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return post(url, headers, body, responseType);
    }

    /**
     * GitHub API에 인증 없이 POST 요청을 보내고 응답을 지정된 타입으로 반환합니다.
     * @param url           요청을 보낼 GitHub API의 URL
     * @param headers       요청 헤더
     * @param body          요청 본문
     * @param responseType  응답 본문의 타입 클래스
     * @return              GitHub API의 응답 본문
     * @param <T>           응답 본문의 타입
     */
    public <T> T postNoAuth(String url, HttpHeaders headers, Object body, Class<T> responseType) {
        return post(url, headers, body, responseType);
    }

    private <T> T post(String url, HttpHeaders headers, Object body, Class<T> responseType) {
        HttpEntity<Object> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    responseType
            );

            return response.getBody();
        } catch (RestClientResponseException e) {
            log.error("Error during POST request to GitHub API. URL = {}, Status code = {}, Response body = {}",
                    url,
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }
}
