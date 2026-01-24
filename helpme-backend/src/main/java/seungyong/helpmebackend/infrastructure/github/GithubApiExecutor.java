package seungyong.helpmebackend.infrastructure.github;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GithubRateLimitException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubApiExecutor {
    private final GithubClient githubClient;
    private final ObjectMapper objectMapper;

    @FunctionalInterface
    public interface JsonResponseParser<T> {
        T parse(JsonNode jsonNode) throws Exception;
    }

    @FunctionalInterface
    public interface ExceptionHandler<T> {
        Optional<T> handle(Exception e);
    }

    public <T> T executeGet(
            String url,
            String accessToken,
            JsonResponseParser<T> parser,
            String operationName,
            ExceptionHandler<T> exceptionHandler
    ) {
        String responseBody = null;

        try {
            responseBody = githubClient.fetchGetMethodForBody(url, accessToken);
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return parser.parse(jsonNode);
        } catch (Exception e) {
            String errorResponseBody = extractResponseBody(e, responseBody);

            if (isCommonHttpError(e)) {
                handleCommonHttpException(e, errorResponseBody, operationName);
            }

            return executeWithExceptionHandler(e, errorResponseBody, operationName, exceptionHandler);
        }
    }

    public <T> T executeGet(
            String url,
            String accessToken,
            JsonResponseParser<T> parser,
            String operationName
    ) {
        return executeGet(url, accessToken, parser, operationName, null);
    }

    public <T> T executeGetJson(
            String url,
            String accessToken,
            String accept,
            Function<ResponseEntity<String>, T> handler,
            String operationName
    ) {
        ResponseEntity<String> response = null;

        try {
            response = githubClient.fetchGet(
                    url,
                    accessToken,
                    accept,
                    String.class
            );

            return handler.apply(response);
        } catch (Exception e) {
            String responseBody = extractResponseBody(e, response);

            if (isCommonHttpError(e)) {
                handleCommonHttpException(e, responseBody, operationName);
            }

            return executeWithExceptionHandler(e, responseBody, operationName, null);
        }
    }

    public <T> T executePost(
            String url,
            String accessToken,
            Map<String, String> requestBody,
            JsonResponseParser<T> parser,
            String operationName
    ) {
        try {
            String responseBody = githubClient.postWithBearer(url, accessToken, requestBody, String.class);
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return parser.parse(jsonNode);
        } catch (Exception e) {
            String errorResponseBody = extractResponseBody(e, null);

            if (isCommonHttpError(e)) {
                handleCommonHttpException(e, errorResponseBody, operationName);
            }

            return executeWithExceptionHandler(e, errorResponseBody, operationName, null);
        }
    }

    public <T> T executePostNoAuth(
            String url,
            Map<String, String> requestBody,
            Class<T> responseType,
            String operationName
    ) {
        try {
            return githubClient.postNoAuth(url, requestBody, responseType);
        } catch (Exception e) {
            String errorResponseBody = extractResponseBody(e, null);

            if (isCommonHttpError(e)) {
                handleCommonHttpException(e, errorResponseBody, operationName);
            }

            return executeWithExceptionHandler(e, errorResponseBody, operationName, null);
        }
    }

    public void executePut(
            String url,
            String accessToken,
            Map<String, String> requestBody,
            String operationName,
            ExceptionHandler<Void> exceptionHandler
    ) {
        try {
            githubClient.putWithBearer(url, accessToken, requestBody);
        } catch (Exception e) {
            String responseBody = extractResponseBody(e, null);

            if (isCommonHttpError(e)) {
                handleCommonHttpException(e, responseBody, operationName);
            }

            executeWithExceptionHandler(e, responseBody, operationName, exceptionHandler);
        }
    }

    public void executePut(
            String url,
            String accessToken,
            Map<String, String> requestBody,
            String operationName
    ) {
        executePut(url, accessToken, requestBody, operationName, null);
    }

    public void executeDelete(
            String url,
            String accessToken,
            String operationName
    ) {
        try {
            githubClient.deleteWithBearer(url, accessToken);
        } catch (Exception e) {
            String responseBody = extractResponseBody(e, null);

            if (isCommonHttpError(e)) {
                handleCommonHttpException(e, responseBody, operationName);
            }

            executeWithExceptionHandler(e, responseBody, operationName, null);
        }
    }

    /**
     * 예외 또는 응답에서 응답 본문 추출
     */
    private String extractResponseBody(Exception e, Object fallback) {
        // HttpClientErrorException에서 직접 추출
        if (e instanceof HttpClientErrorException httpEx) {
            return httpEx.getResponseBodyAsString();
        }

        // ResponseEntity에서 추출
        if (fallback instanceof ResponseEntity<?> response) {
            Object body = response.getBody();
            return body != null ? body.toString() : null;
        }

        // 이미 String인 경우
        if (fallback instanceof String str) {
            return str;
        }

        return null;
    }

    private <T> T executeWithExceptionHandler(
            Exception e,
            String responseBody,
            String operationName,
            ExceptionHandler<T> exceptionHandler
    ) {
        if (exceptionHandler != null) {
            Optional<T> result = exceptionHandler.handle(e);

            if (result.isEmpty()) {
                handleException(e, responseBody, operationName);
                return null;
            }

            return result.get();
        }

        handleException(e, responseBody, operationName);
        return null;
    }

    private boolean isCommonHttpError(Exception e) {
        if (!(e instanceof HttpClientErrorException httpEx)) {
            return false;
        }

        HttpStatusCode status = httpEx.getStatusCode();

        return status == HttpStatus.UNAUTHORIZED ||
                status == HttpStatus.FORBIDDEN ||
                status == HttpStatus.TOO_MANY_REQUESTS;
    }

    private void handleCommonHttpException(Exception e, String responseBody, String operationName) {
        HttpClientErrorException httpEx = (HttpClientErrorException) e;
        HttpStatusCode status = httpEx.getStatusCode();

        if (status == HttpStatus.UNAUTHORIZED) {
            log.error("[{}] Unauthorized access to Github API. Response = {}", operationName, responseBody, e);
            throw new CustomException(RepositoryErrorCode.GITHUB_UNAUTHORIZED);
        }

        if (status == HttpStatus.FORBIDDEN) {
            if (responseBody != null && responseBody.contains("rate limit")) {
                log.error("[{}] Rate limit exceeded for Github API. Response = {}", operationName, responseBody, e);
                throw new GithubRateLimitException(60);
            }

            log.error("[{}] Forbidden access to Github API. Response = {}", operationName, responseBody, e);
            throw new CustomException(RepositoryErrorCode.GITHUB_FORBIDDEN);
        }

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            HttpHeaders headers = httpEx.getResponseHeaders();
            String resetTime = headers != null ? headers.getFirst("X-RateLimit-Reset") : null;
            String retryAfter = headers != null ? headers.getFirst("Retry-After") : null;

            long waitSeconds = calculateWaitSeconds(resetTime, retryAfter);

            log.error("[{}] Too many requests to Github API. Must wait {} seconds. Response = {}", operationName, waitSeconds, responseBody, e);
            throw new GithubRateLimitException((int) waitSeconds);
        }
    }

    private long calculateWaitSeconds(String resetTime, String retryAfter) {
        try {
            if (retryAfter != null) {
                return Long.parseLong(retryAfter);
            }

            if (resetTime != null) {
                long resetEpoch = Long.parseLong(resetTime);
                return resetEpoch - (System.currentTimeMillis() / 1000);
            }
        } catch (NumberFormatException ex) {
            log.warn("Failed to parse rate limit headers: resetTime={}, retryAfter={}", resetTime, retryAfter, ex);
        }

        return 60;
    }

    private void handleException(Exception e, String responseBody, String operationName) {
        if (e instanceof JsonParseException) {
            log.error("[{}] JSON parsing error. Response = {}", operationName, responseBody, e);
            throw new CustomException(RepositoryErrorCode.JSON_PROCESSING_ERROR);
        }

        log.error("[{}] Github API error. Response = {}", operationName, responseBody, e);
        throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
    }
}
