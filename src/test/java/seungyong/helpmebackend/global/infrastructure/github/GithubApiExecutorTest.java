package seungyong.helpmebackend.global.infrastructure.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GithubApiExecutorTest {
    @Mock private GithubClient githubClient;
    @Mock private GithubRateLimitGuard rateLimitGuard;

    private GithubApiExecutor githubApiExecutor;

    @BeforeEach
    void setUp() {
        githubApiExecutor = new GithubApiExecutor(
                githubClient,
                new ObjectMapper(),
                rateLimitGuard
        );
    }

    @Test
    @DisplayName("GET 응답을 파싱한다")
    void executeGet_success() {
        given(githubClient.fetchGetMethodForBody("url", "token"))
                .willReturn("{\"key\":\"value\"}");

        String result = githubApiExecutor.executeGet(
                "url",
                "token",
                json -> json.get("key").asText(),
                "test operation"
        );

        assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("GitHub HTTP 상태를 공급자 예외에 보존한다")
    void executeGet_preservesHttpStatus() {
        given(githubClient.fetchGetMethodForBody(anyString(), anyString()))
                .willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> githubApiExecutor.executeGet(
                1L, "url", "token", json -> "value", "test operation"
        ))
                .isInstanceOfSatisfying(GithubApiException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(401);
                    assertThat(exception.isRateLimited()).isFalse();
                });
    }

    @Test
    @DisplayName("403 rate limit 응답과 재시도 시간을 식별한다")
    void executeGet_detectsRateLimit() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Remaining", "0");
        headers.set(HttpHeaders.RETRY_AFTER, "17");
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                headers,
                "{\"message\":\"API rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willThrow(exception);

        assertThatThrownBy(() -> githubApiExecutor.executeGet(
                1L, "url", "token", json -> "value", "test operation"
        ))
                .isInstanceOfSatisfying(GithubApiException.class, githubException -> {
                    assertThat(githubException.isRateLimited()).isTrue();
                    assertThat(githubException.getRetryAfterSeconds()).isEqualTo(17);
                });
        verify(rateLimitGuard).block(1L, 17);
    }

    @Test
    @DisplayName("사용자의 rate limit TTL이 남아 있으면 GitHub를 호출하지 않는다")
    void executeGet_rejectsCachedRateLimitBeforeCallingGithub() {
        given(rateLimitGuard.getRetryAfterSeconds(1L)).willReturn(OptionalInt.of(23));

        assertThatThrownBy(() -> githubApiExecutor.executeGet(
                1L, "url", "token", json -> "value", "test operation"
        )).isInstanceOfSatisfying(GithubApiException.class, exception -> {
            assertThat(exception.getStatusCode().value()).isEqualTo(429);
            assertThat(exception.isRateLimited()).isTrue();
            assertThat(exception.getRetryAfterSeconds()).isEqualTo(23);
        });

        verifyNoInteractions(githubClient);
    }

    @Test
    @DisplayName("유효하지 않은 JSON은 응답 파싱 예외로 변환한다")
    void executeGet_wrapsInvalidJson() {
        given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willReturn("not-json");

        assertThatThrownBy(() -> githubApiExecutor.executeGet(
                "url", "token", json -> "value", "test operation"
        )).isInstanceOf(GithubResponseParsingException.class);
    }

    @Test
    @DisplayName("파서 오류는 응답 파싱 예외로 변환한다")
    void executeGet_wrapsParserFailure() {
        given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willReturn("{}");

        assertThatThrownBy(() -> githubApiExecutor.executeGet(
                "url",
                "token",
                json -> { throw new IllegalArgumentException("missing field"); },
                "test operation"
        )).isInstanceOf(GithubResponseParsingException.class);
    }

    @Test
    @DisplayName("POST 성공 응답을 공통 실행 경로에서 파싱한다")
    void executePost_success() {
        given(githubClient.postWithBearer(anyString(), anyString(), anyMap(), any()))
                .willReturn("{\"number\":100}");

        Integer result = githubApiExecutor.executePost(
                "url", "token", Map.of("title", "hello"),
                json -> json.get("number").asInt(), "post operation"
        );

        assertThat(result).isEqualTo(100);
    }

    @Test
    @DisplayName("raw GET 응답을 파싱 없이 반환한다")
    void executeGetRaw_success() {
        given(githubClient.fetchGetMethodForBody("url", "token", "application/vnd.github.raw+json"))
                .willReturn("# README");

        assertThat(githubApiExecutor.executeGetRaw("url", "token", "raw operation"))
                .isEqualTo("# README");
    }

    @Test
    @DisplayName("인증 없는 POST 응답을 그대로 반환한다")
    void executePostNoAuth_success() {
        given(githubClient.postNoAuth("url", Map.of("code", "value"), String.class))
                .willReturn("token-result");

        assertThat(githubApiExecutor.executePostNoAuth(
                "url", Map.of("code", "value"), String.class, "oauth operation"
        )).isEqualTo("token-result");
    }

    @Test
    @DisplayName("예상하지 못한 클라이언트 오류도 공급자 예외 경계를 유지한다")
    void executePost_wrapsUnexpectedFailure() {
        given(githubClient.postWithBearer(anyString(), anyString(), anyMap(), any()))
                .willThrow(new IllegalStateException("unexpected"));

        assertThatThrownBy(() -> githubApiExecutor.executePost(
                "url", "token", Map.of(), json -> null, "post operation"
        )).isInstanceOfSatisfying(GithubApiException.class,
                exception -> assertThat(exception.getStatusCode()).isNull());
    }

    @Test
    @DisplayName("PUT 429 응답도 동일한 rate limit 예외로 변환한다")
    void executePut_detectsRateLimit() {
        doThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
                .when(githubClient).putWithBearer(anyString(), anyString(), anyMap());

        assertThatThrownBy(() -> githubApiExecutor.executePut(
                "url", "token", Map.of(), "put operation"
        ))
                .isInstanceOfSatisfying(GithubApiException.class,
                        exception -> assertThat(exception.isRateLimited()).isTrue());
    }

    @Test
    @DisplayName("DELETE 요청을 GitHub 클라이언트에 위임한다")
    void executeDelete_success() {
        githubApiExecutor.executeDelete("url", "token", "delete operation");

        verify(githubClient).deleteWithBearer("url", "token");
    }

    @Test
    @DisplayName("DELETE HTTP 오류도 상태 코드를 보존한다")
    void executeDelete_preservesHttpStatus() {
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(githubClient).deleteWithBearer(anyString(), anyString());

        assertThatThrownBy(() -> githubApiExecutor.executeDelete(
                "url", "token", "delete operation"
        )).isInstanceOfSatisfying(GithubApiException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(404));
    }
}
