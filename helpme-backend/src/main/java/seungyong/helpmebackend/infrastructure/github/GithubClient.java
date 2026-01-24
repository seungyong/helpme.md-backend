package seungyong.helpmebackend.infrastructure.github;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.github.dto.PageInfo;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GithubClient {
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
     * GitHub API 응답 헤더에서 'last' 페이지와 중간 페이지 번호를 추출합니다.
     *
     * @param headers   GitHub API 응답 헤더
     * @return          PageInfo 객체에 'last' 페이지와 중간 페이지 번호를 담아 반환
     */
    public static PageInfo extractLastAndMiddlePage(HttpHeaders headers) {
        String link = headers.getFirst(HttpHeaders.LINK);
        if (link.isEmpty()) {
            return new PageInfo(null, null);
        }

        Map<String, String> relToUrl = parseLinkHeader(link);
        String lastUrl = relToUrl.get("last");
        if (lastUrl == null) {
            // last가 없으면 페이지 1개 뿐이거나 마지막 정보가 없는 경우
            return new PageInfo(1, 1);
        }

        Integer lastPage = extractPageQueryParam(lastUrl);
        if (lastPage == null || lastPage < 1) {
            return new PageInfo(null, null);
        }

        int middlePage = (lastPage / 2) + (lastPage % 2);
        return new PageInfo(lastPage, middlePage);
    }

    private static Map<String, String> parseLinkHeader(String linkHeader) {
        Map<String, String> result = new HashMap<>();
        String[] parts = linkHeader.split(",");

        for (String part : parts) {
            String[] sections = part.trim().split(";");
            if (sections.length < 2) continue;

            String urlPart = sections[0].trim();
            String relPart = sections[1].trim();

            if (urlPart.startsWith("<") && urlPart.endsWith(">")) {
                urlPart = urlPart.substring(1, urlPart.length() - 1);
            }

            String rel = null;
            if (relPart.startsWith("rel=\"") && relPart.endsWith("\"")) {
                rel = relPart.substring(5, relPart.length() - 1);
            }

            if (rel != null) {
                result.put(rel, urlPart);
            }
        }
        return result;
    }

    private static Integer extractPageQueryParam(String url) {
        try {
            URI uri = URI.create(url);
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                return null;
            }

            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && kv[0].equals("page")) {
                    return Integer.parseInt(kv[1]);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
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
    public <T> T postNoAuth(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

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

    /**
     * GitHub API에 PUT 요청을 보내는 메서드입니다.
     *
     * @param url           요청을 보낼 GitHub API의 URL
     * @param token         인증에 사용할 Bearer 토큰
     * @param body          요청 본문
     * @param <T>           응답 본문의 타입
     */
    public <T> void putWithBearer(String url, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        put(url, headers, body);
    }

    private <T> void put(String url, HttpHeaders headers, Object body) {
        headers.set(HttpHeaders.ACCEPT, Accept.APPLICATION_GITHUB_VND_GITHUB_JSON);
        headers.set("X-GitHub-Api-Version", API_VERSION);

        HttpEntity<Object> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    Void.class
            );
        } catch (RestClientResponseException e) {
            log.error("Error during PUT request to GitHub API. URL = {}, Status code = {}, Response body = {}",
                    url,
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    /**
     * GitHub API에 DELETE 요청을 보내는 메서드입니다.
     *
     * @param url       요청을 보낼 GitHub API의 URL
     * @param token     인증에 사용할 Bearer 토큰
     */
    public void deleteWithBearer(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        delete(url, headers);
    }

    private void delete(String url, HttpHeaders headers) {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        headers.set(HttpHeaders.ACCEPT, Accept.APPLICATION_GITHUB_VND_GITHUB_JSON);
        headers.set("X-GitHub-Api-Version", API_VERSION);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );
        } catch (RestClientResponseException e) {
            log.error("Error during DELETE request to GitHub API. URL = {}, Status code = {}, Response body = {}",
                    url,
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }
}
