package seungyong.helpmebackend.global.infrastructure.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GithubRateLimitException;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubApiExecutorTest {
    @Mock private GithubClient githubClient;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private GithubApiExecutor githubApiExecutor;

    @Nested
    @DisplayName("executeGet - GET 요청 실행")
    class ExecuteGet {
        @Test
        @DisplayName("성공")
        void executeGet_success() throws Exception {
            String url = "https://api.github.com/test";
            String token = "token";
            String responseBody = "{\"key\":\"value\"}";
            JsonNode mockJsonNode = mock(JsonNode.class);

            given(githubClient.fetchGetMethodForBody(url, token)).willReturn(responseBody);
            given(objectMapper.readTree(responseBody)).willReturn(mockJsonNode);

            String result = githubApiExecutor.executeGet(url, token, node -> "parsedValue", "testOp");

            assertThat(result).isEqualTo("parsedValue");
        }

        @Test
        @DisplayName("성공 (ExceptionHandler로 예외 복구)")
        void executeGet_success_withExceptionHandler() {
            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willThrow(new RuntimeException());

            String recoveredValue = githubApiExecutor.executeGet(
                    "url", "token", node -> "val", "testOp",
                    e -> Optional.of("recovered")
            );

            assertThat(recoveredValue).isEqualTo("recovered");
        }

        @Test
        @DisplayName("실패 (401 미인증)")
        void executeGet_failure_unauthorized() {
            String url = "https://api.github.com/test";
            HttpClientErrorException unauthorizedException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willThrow(unauthorizedException);

            assertThatThrownBy(() -> githubApiExecutor.executeGet(url, "token", node -> "val", "testOp"))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("executeGet - 실패 (403 Rate Limit)")
        void executeGet_failure_rateLimit() {
            String url = "https://api.github.com/test";
            String rateLimitBody = "{\"message\":\"API rate limit exceeded\"}";
            HttpClientErrorException forbiddenException = HttpClientErrorException.create(
                    HttpStatus.FORBIDDEN, "Forbidden", null, rateLimitBody.getBytes(), null
            );

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willThrow(forbiddenException);

            assertThatThrownBy(() -> githubApiExecutor.executeGet(url, "token", node -> "val", "testOp"))
                    .isInstanceOf(GithubRateLimitException.class);
        }

        @Test
        @DisplayName("실패 (사용자 정의 예외)")
        void executeGet_failure_customException() {
            String url = "https://api.github.com/test";
            String token = "token";
            HttpClientErrorException notFoundException = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", null, "{\"message\":\"Not Found\"}".getBytes(), null
            );

            given(githubClient.fetchGetMethodForBody(url, token)).willThrow(notFoundException);

            assertThatThrownBy(
                    () -> githubApiExecutor.executeGet(url, token, node -> null, "testOp", e -> {
                        throw new CustomException(RepositoryErrorCode.BRANCH_NOT_FOUND);
                    })
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.BRANCH_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("executePost - POST 요청 실행")
    class ExecutePost {
        @Test
        @DisplayName("성공")
        void executePost_success() throws Exception {
            Map<String, String> body = Map.of("title", "hello");
            String responseStr = "{}";
            JsonNode mockNode = mock(JsonNode.class);

            given(githubClient.postWithBearer(anyString(), anyString(), anyMap(), any())).willReturn(responseStr);
            given(objectMapper.readTree(responseStr)).willReturn(mockNode);

            Integer result = githubApiExecutor.executePost("url", "token", body, node -> 100, "postOp");

            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("실패 (일반 에러)")
        void executePost_failure() {
            given(githubClient.postWithBearer(anyString(), anyString(), anyMap(), any())).willThrow(new RuntimeException());

            assertThatThrownBy(() -> githubApiExecutor.executePost("url", "token", Map.of(), node -> 1, "postOp"))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("executePut - PUT 요청 실행")
    class ExecutePut {
        @Test
        @DisplayName("성공")
        void executePut_success() {
            githubApiExecutor.executePut("url", "token", Map.of(), "putOp");

            verify(githubClient, times(1)).putWithBearer(anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("실패 (429 Too Many Requests)")
        void executePut_failure_tooManyRequests() {
            HttpClientErrorException tooManyRequestsException = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);

            doThrow(tooManyRequestsException).when(githubClient).putWithBearer(anyString(), anyString(), anyMap());

            assertThatThrownBy(() -> githubApiExecutor.executePut("url", "token", Map.of(), "putOp"))
                    .isInstanceOf(GithubRateLimitException.class);
        }
    }

    @Nested
    @DisplayName("executeDelete - DELETE 요청 실행")
    class ExecuteDelete {

        @Test
        @DisplayName("성공")
        void executeDelete_success() {
            githubApiExecutor.executeDelete("url", "token", "deleteOp");

            verify(githubClient, times(1)).deleteWithBearer(anyString(), anyString());
        }

        @Test
        @DisplayName("실패")
        void executeDelete_failure() {
            doThrow(new RuntimeException()).when(githubClient).deleteWithBearer(anyString(), anyString());

            assertThatThrownBy(() -> githubApiExecutor.executeDelete("url", "token", "deleteOp"))
                    .isInstanceOf(CustomException.class);
        }
    }
}