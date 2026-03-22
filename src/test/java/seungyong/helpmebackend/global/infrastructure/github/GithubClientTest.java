package seungyong.helpmebackend.global.infrastructure.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import seungyong.helpmebackend.global.domain.entity.PageInfo;
import seungyong.helpmebackend.global.exception.CustomException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GithubClientTest {

    @InjectMocks
    private GithubClient githubClient;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // 내부에서 생성된 restTemplate을 mock 객체로 교체
        ReflectionTestUtils.setField(githubClient, "restTemplate", restTemplate);
    }

    @Nested
    @DisplayName("extractNextUrl - 다음 페이지 URL 추출")
    class ExtractNextUrl {
        @Test
        @DisplayName("성공")
        void extractNextUrl_success() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.LINK, "<https://api.github.com/user/repos?page=2>; rel=\"next\", <https://api.github.com/user/repos?page=5>; rel=\"last\"");

            Optional<String> result = GithubClient.extractNextUrl(headers);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("https://api.github.com/user/repos?page=2");
        }

        @Test
        @DisplayName("실패 (Link 헤더 없음)")
        void extractNextUrl_failure_noLinkHeader() {
            HttpHeaders headers = new HttpHeaders();

            Optional<String> result = GithubClient.extractNextUrl(headers);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractLastAndMiddlePage - 페이지 정보 추출")
    class ExtractLastAndMiddlePage {
        @Test
        @DisplayName("성공")
        void extractLastAndMiddlePage_success() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.LINK, "<https://api.github.com/user/repos?page=10>; rel=\"last\"");

            PageInfo result = GithubClient.extractLastAndMiddlePage(headers);

            assertThat(result.lastPage()).isEqualTo(10);
            assertThat(result.middlePage()).isEqualTo(5);
        }

        @Test
        @DisplayName("성공 (홀수 페이지)")
        void extractLastAndMiddlePage_success_oddPage() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.LINK, "<https://api.github.com/user/repos?page=7>; rel=\"last\"");

            PageInfo result = GithubClient.extractLastAndMiddlePage(headers);

            assertThat(result.lastPage()).isEqualTo(7);
            assertThat(result.middlePage()).isEqualTo(4); // (7/2) + (7%2) = 3 + 1
        }

        @Test
        @DisplayName("실패 (페이지 번호 없음)")
        void extractLastAndMiddlePage_failure_noPageParam() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.LINK, "<https://api.github.com/user/repos>; rel=\"last\"");

            PageInfo result = GithubClient.extractLastAndMiddlePage(headers);

            assertThat(result.lastPage()).isNull();
        }
    }

    @Nested
    @DisplayName("fetchGet - GET 요청 실행")
    class FetchGet {
        @Test
        @DisplayName("성공")
        void fetchGet_success() {
            String url = "url";
            ResponseEntity<String> response = ResponseEntity.ok("body");
            given(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .willReturn(response);

            ResponseEntity<String> result = githubClient.fetchGet(url, "token", "accept", String.class);

            assertThat(result.getBody()).isEqualTo("body");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("postWithBearer - POST 요청 실행")
    class PostWithBearer {
        @Test
        @DisplayName("성공")
        void postWithBearer_success() {
            String url = "url";
            Map<String, String> body = Map.of("data", "test");
            given(restTemplate.exchange(eq(url), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .willReturn(ResponseEntity.ok("success"));

            String result = githubClient.postWithBearer(url, "token", body, String.class);

            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("실패 (API 오류)")
        void postWithBearer_failure_apiError() {
            given(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    any(Class.class)
            )).willThrow(new RestClientResponseException("error", 500, "Internal Server Error", null, null, null));

            assertThatThrownBy(() -> githubClient.postWithBearer("url", "token", Map.of(), String.class))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("deleteWithBearer - DELETE 요청 실행")
    class DeleteWithBearer {
        @Test
        @DisplayName("성공")
        void deleteWithBearer_success() {
            githubClient.deleteWithBearer("url", "token");

            verify(restTemplate).exchange(eq("url"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
        }

        @Test
        @DisplayName("실패")
        void deleteWithBearer_failure() {
            given(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                    .willThrow(new RestClientResponseException("error", 404, "Not Found", null, null, null));

            assertThatThrownBy(() -> githubClient.deleteWithBearer("url", "token"))
                    .isInstanceOf(CustomException.class);
        }
    }
}