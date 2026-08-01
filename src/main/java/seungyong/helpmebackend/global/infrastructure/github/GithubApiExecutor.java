package seungyong.helpmebackend.global.infrastructure.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import seungyong.helpmebackend.global.exception.CustomException;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubApiExecutor {
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 60;

    private final GithubClient githubClient;
    private final ObjectMapper objectMapper;
    private final GithubRateLimitGuard rateLimitGuard;

    @FunctionalInterface
    public interface JsonResponseParser<T> {
        T parse(JsonNode jsonNode) throws Exception;
    }

    @FunctionalInterface
    private interface GithubOperation<T> {
        T execute() throws Exception;
    }

    /**
     * GitHub API GET 요청을 수행하고, JSON 응답을 파싱하여 결과를 반환합니다.
     * UserId를 제공하지 않기 떄문에, rate limit이 발생하면, rate limit 정보를 제공하지 않습니다.
     *
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param parser        JSON 응답을 파싱하는 함수형 인터페이스
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @param <T>           반환 타입
     * @return 파싱된 결과 객체
     */
    public <T> T executeGet(
            String url,
            String accessToken,
            JsonResponseParser<T> parser,
            String operationName
    ) {
        return executeGet(null, url, accessToken, parser, operationName);
    }

    /**
     * GitHub API GET 요청을 수행하고, JSON 응답을 파싱하여 결과를 반환합니다.
     *
     * @param userId        사용자 ID (rate limit 관리에 사용)
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param parser        JSON 응답을 파싱하는 함수형 인터페이스
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @param <T>           반환 타입
     * @return 파싱된 결과 객체
     */
    public <T> T executeGet(
            Long userId,
            String url,
            String accessToken,
            JsonResponseParser<T> parser,
            String operationName
    ) {
        return execute(userId, operationName, () -> parse(
                githubClient.fetchGetMethodForBody(url, accessToken),
                parser
        ));
    }

    /**
     * GitHub API GET 요청을 수행하고, 파일 내용을 그대로 반환합니다. (예: README.md 파일 내용)
     * UserId를 제공하지 않기 떄문에, rate limit이 발생하면, rate limit 정보를 제공하지 않습니다.
     *
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @return 파일 내용 문자열
     */
    public String executeGetRaw(
            String url,
            String accessToken,
            String operationName
    ) {
        return executeGetRaw(null, url, accessToken, operationName);
    }

    /**
     * GitHub API GET 요청을 수행하고, 파일 내용을 그대로 반환합니다. (예: README.md 파일 내용)
     *
     * @param userId        사용자 ID (rate limit 관리에 사용)
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @return 파일 내용 문자열
     */
    public String executeGetRaw(
            Long userId,
            String url,
            String accessToken,
            String operationName
    ) {
        return execute(userId, operationName, () -> githubClient.fetchGetMethodForBody(
                url,
                accessToken,
                GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_RAW_JSON
        ));
    }

    /**
     * GitHub API GET 요청을 수행하고, JSON 응답을 처리하는 핸들러를 적용하여 결과를 반환합니다.
     * UserId를 제공하지 않기 떄문에, rate limit이 발생하면, rate limit 정보를 제공하지 않습니다.
     *
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param accept        요청 헤더의 Accept 값
     * @param handler       JSON 응답을 처리하는 함수형 인터페이스
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @param <T>           반환 타입
     * @return 핸들러가 처리한 결과 객체
     */
    public <T> T executeGetJson(
            String url,
            String accessToken,
            String accept,
            Function<ResponseEntity<String>, T> handler,
            String operationName
    ) {
        return executeGetJson(null, url, accessToken, accept, handler, operationName);
    }

    /**
     * GitHub API GET 요청을 수행하고, JSON 응답을 처리하는 핸들러를 적용하여 결과를 반환합니다.
     *
     * @param userId        사용자 ID (rate limit 관리에 사용)
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param accept        요청 헤더의 Accept 값
     * @param handler       JSON 응답을 처리하는 함수형 인터페이스
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @param <T>           반환 타입
     * @return 핸들러가 처리한 결과 객체
     */
    public <T> T executeGetJson(
            Long userId,
            String url,
            String accessToken,
            String accept,
            Function<ResponseEntity<String>, T> handler,
            String operationName
    ) {
        return execute(userId, operationName, () -> handler.apply(githubClient.fetchGet(
                url,
                accessToken,
                accept,
                String.class
        )));
    }

    /**
     * GitHub API POST 요청을 수행하고, JSON 응답을 지정된 타입으로 반환합니다.
     * UserId를 제공하지 않기 떄문에, rate limit이 발생하면, rate limit 정보를 제공하지 않습니다.
     *
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param requestBody   요청 본문 데이터 (Map 형태)
     * @param parser        JSON 응답을 매핑할 함수형 인터페이스
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @param <T>           반환 타입
     * @return JSON 응답을 매핑한 결과 객체
     */
    public <T> T executePost(
            String url,
            String accessToken,
            Map<String, String> requestBody,
            JsonResponseParser<T> parser,
            String operationName
    ) {
        return executePost(null, url, accessToken, requestBody, parser, operationName);
    }

    /**
     * GitHub API POST 요청을 수행하고, JSON 응답을 지정된 타입으로 반환합니다.
     *
     * @param userId        사용자 ID (rate limit 관리에 사용)
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param requestBody   요청 본문 데이터 (Map 형태)
     * @param parser        JSON 응답을 매핑할 함수형 인터페이스
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @param <T>           반환 타입
     * @return JSON 응답을 매핑한 결과 객체
     */
    public <T> T executePost(
            Long userId,
            String url,
            String accessToken,
            Map<String, String> requestBody,
            JsonResponseParser<T> parser,
            String operationName
    ) {
        return execute(userId, operationName, () -> parse(
                githubClient.postWithBearer(url, accessToken, requestBody, String.class),
                parser
        ));
    }

    /**
     * 인증 없이 GitHub API POST 요청을 수행하고, JSON 응답을 지정된 타입으로 반환합니다.
     * UserId를 제공하지 않기 떄문에, rate limit이 발생하면, rate limit 정보를 제공하지 않습니다.
     *
     * @param url           GitHub API 엔드포인트 URL
     * @param requestBody   요청 본문 데이터 (Map 형태)
     * @param responseType  JSON 응답을 매핑할 클래스 타입
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     * @param <T>           반환 타입
     * @return JSON 응답을 매핑한 결과 객체
     */
    public <T> T executePostNoAuth(
            String url,
            Map<String, String> requestBody,
            Class<T> responseType,
            String operationName
    ) {
        return execute(null, operationName, () -> githubClient.postNoAuth(url, requestBody, responseType));
    }

    /**
     * GitHub API PUT 요청을 수행합니다.
     *
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param requestBody   요청 본문 데이터 (Map 형태)
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     */
    public void executePut(
            String url,
            String accessToken,
            Map<String, String> requestBody,
            String operationName
    ) {
        executePut(null, url, accessToken, requestBody, operationName);
    }

    /**
     * GitHub API PUT 요청을 수행합니다.
     *
     * @param userId        사용자 ID (rate limit 관리에 사용)
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param requestBody   요청 본문 데이터 (Map 형태)
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     */
    public void executePut(
            Long userId,
            String url,
            String accessToken,
            Map<String, String> requestBody,
            String operationName
    ) {
        execute(userId, operationName, () -> {
            githubClient.putWithBearer(url, accessToken, requestBody);
            return null;
        });
    }

    /**
     * GitHub API DELETE 요청을 수행합니다.
     * UserId를 제공하지 않기 떄문에, rate limit이 발생하면, rate limit 정보를 제공하지 않습니다.
     *
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     */
    public void executeDelete(
            String url,
            String accessToken,
            String operationName
    ) {
        executeDelete(null, url, accessToken, operationName);
    }

    /**
     * GitHub API DELETE 요청을 수행합니다.
     *
     * @param userId        사용자 ID (rate limit 관리에 사용)
     * @param url           GitHub API 엔드포인트 URL
     * @param accessToken   GitHub 액세스 토큰
     * @param operationName 수행되는 작업의 이름 (로깅 및 예외 처리에 사용)
     */
    public void executeDelete(
            Long userId,
            String url,
            String accessToken,
            String operationName
    ) {
        execute(userId, operationName, () -> {
            githubClient.deleteWithBearer(url, accessToken);
            return null;
        });
    }

    private <T> T parse(String responseBody, JsonResponseParser<T> parser) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        try {
            return parser.parse(jsonNode);
        } catch (CustomException | GithubResponseParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new GithubResponseParsingException(e);
        }
    }

    private <T> T execute(Long userId, String operationName, GithubOperation<T> operation) {
        OptionalInt cachedRetryAfterSeconds = rateLimitGuard.getRetryAfterSeconds(userId);
        if (cachedRetryAfterSeconds.isPresent()) {
            throw new GithubApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    true,
                    cachedRetryAfterSeconds.getAsInt(),
                    null
            );
        }

        try {
            return operation.execute();
        } catch (CustomException | GithubApiException | GithubResponseParsingException e) {
            throw e;
        } catch (RestClientResponseException e) {
            GithubApiException githubException = toGithubApiException(e);
            if (githubException.isRateLimited()) {
                rateLimitGuard.block(userId, githubException.getRetryAfterSeconds());
            }
            log.warn(
                    "GitHub API operation failed: operation={}, status={}, rateLimited={}, retryAfterSeconds={}",
                    operationName,
                    e.getStatusCode().value(),
                    githubException.isRateLimited(),
                    githubException.getRetryAfterSeconds()
            );
            throw githubException;
        } catch (JsonProcessingException e) {
            log.warn("GitHub API returned an invalid JSON response: operation={}", operationName);
            throw new GithubResponseParsingException(e);
        } catch (Exception e) {
            log.error(
                    "GitHub API operation failed unexpectedly: operation={}, exceptionType={}",
                    operationName,
                    e.getClass().getSimpleName()
            );
            throw new GithubApiException(null, false, null, e);
        }
    }

    private GithubApiException toGithubApiException(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        boolean rateLimited = status.value() == HttpStatus.TOO_MANY_REQUESTS.value()
                || isRateLimitedForbidden(exception);
        int retryAfterSeconds = rateLimited
                ? calculateRetryAfterSeconds(exception.getResponseHeaders())
                : DEFAULT_RETRY_AFTER_SECONDS;

        return new GithubApiException(status, rateLimited, retryAfterSeconds, exception);
    }

    private boolean isRateLimitedForbidden(RestClientResponseException exception) {
        if (exception.getStatusCode().value() != HttpStatus.FORBIDDEN.value()) {
            return false;
        }

        HttpHeaders headers = exception.getResponseHeaders();

        // X-RateLimit-Remaining은 GitHub API의 rate limit 상태를 나타내는 헤더로, 남은 요청 수를 나타냄
        String remaining = headers == null ? null : headers.getFirst("X-RateLimit-Remaining");
        if ("0".equals(remaining)) {
            return true;
        }

        String responseBody = exception.getResponseBodyAsString();
        // GitHub API의 응답 본문에 "rate limit" 문자열이 포함되어 있는지 확인하여 rate limit 여부를 판단
        return responseBody.toLowerCase(Locale.ROOT).contains("rate limit");
    }

    private int calculateRetryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }

        // GitHub API의 Retry-After 헤더를 확인하여 rate limit 해제까지 남은 시간을 계산
        Long retryAfter =
                parseNonNegativeLong(headers.getFirst(HttpHeaders.RETRY_AFTER));

        if (retryAfter != null) {
            return clampToInt(retryAfter);
        }

        // GitHub API의 X-RateLimit-Remaining 헤더를 확인하여 남은 요청 수가 0인 경우, X-RateLimit-Reset 헤더를 사용하여 rate limit 해제까지 남은 시간을 계산
        String remaining = headers.getFirst("X-RateLimit-Remaining");

        if ("0".equals(remaining)) {
            Long resetEpoch =
                    parseNonNegativeLong(headers.getFirst("X-RateLimit-Reset"));

            if (resetEpoch != null) {
                long waitSeconds =
                        Math.max(0, resetEpoch - Instant.now().getEpochSecond());

                return clampToInt(waitSeconds);
            }
        }

        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    private Long parseNonNegativeLong(String value) {
        if (value == null) {
            return null;
        }

        try {
            long parsed = Long.parseLong(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int clampToInt(long value) {
        return (int) Math.min(Math.max(1, value), Integer.MAX_VALUE);
    }
}
