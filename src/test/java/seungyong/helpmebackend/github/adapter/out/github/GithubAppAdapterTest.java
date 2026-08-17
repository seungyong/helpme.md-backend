package seungyong.helpmebackend.github.adapter.out.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GithubRateLimitException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GithubAppAdapterTest {
    private static final Long USER_ID = 1L;
    private static final String TOKEN = "github-token";
    private static final Long INSTALLATION_ID = 9001L;

    @Mock private GithubApiExecutor githubApiExecutor;
    private GithubAppAdapter githubAppAdapter;

    @BeforeEach
    void setUp() {
        githubAppAdapter = new GithubAppAdapter(githubApiExecutor, new ObjectMapper());
    }

    @Test
    @DisplayName("개인·Organization installation pagination과 Repository 수를 조합")
    void getInstallations_success_pagination() {
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            if (url.contains("page=2")) {
                return handle(invocation, ResponseEntity.ok("""
                        {"installations":[{
                          "id":9002,
                          "account":{"login":"helpme-org","type":"Organization"},
                          "repository_selection":"all"
                        }]}
                        """));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LINK,
                    "<https://api.github.com/user/installations?per_page=100&page=2>; rel=\"next\"");
            return handle(invocation, new ResponseEntity<>("""
                    {"installations":[{
                      "id":9001,
                      "account":{"login":"seungyong","type":"User"},
                      "repository_selection":"selected"
                    }]}
                    """, headers, HttpStatus.OK));
        });
        given(githubApiExecutor.executeGet(
                anyLong(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            int count = url.contains("9001") ? 8 : 3;
            GithubApiExecutor.JsonResponseParser<?> parser = invocation.getArgument(3);
            return parser.parse(new ObjectMapper().readTree("{\"total_count\":" + count + "}"));
        });

        var result = githubAppAdapter.getInstallations(USER_ID, TOKEN);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).login()).isEqualTo("seungyong");
        assertThat(result.get(0).repositoryCount()).isEqualTo(8);
        assertThat(result.get(1).type().getApiValue()).isEqualTo("Organization");
        assertThat(result.get(1).repositorySelection().getApiValue()).isEqualTo("all");
    }

    @Test
    @DisplayName("검색어가 없으면 GitHub의 요청 페이지 한 개만 조회하고 Branch는 조회하지 않음")
    void getRepositories_success_githubPaginationWithoutBranches() {
        List<String> requestedUrls = new ArrayList<>();
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            requestedUrls.add(url);
            return handle(invocation, ResponseEntity.ok("""
                    {
                      "total_count":25,
                      "repositories":[
                        {"id":101,"full_name":"seungyong/helpme.md","private":true,
                         "default_branch":"main","permissions":{"admin":true,"push":true}},
                        {"id":102,"full_name":"helpme-org/helpme-web","private":false,
                         "default_branch":"develop","permissions":{"admin":false,"push":true}}
                      ]
                    }
                    """));
        });

        var result = githubAppAdapter.getRepositories(
                USER_ID, TOKEN, INSTALLATION_ID, "", 2, 10
        );

        assertThat(requestedUrls).containsExactly(
                "https://api.github.com/user/installations/9001/repositories?per_page=10&page=2"
        );
        assertThat(result.repositories()).hasSize(2);
        assertThat(result.repositories().get(0).privateRepository()).isTrue();
        assertThat(result.repositories().get(1).defaultBranch()).isEqualTo("develop");
        assertThat(result.nextCursor()).isEqualTo("3");
        assertThat(result.hasNext()).isTrue();
        verify(githubApiExecutor, never()).executeGet(
                anyLong(), anyString(), anyString(), any(), anyString()
        );
    }

    @Test
    @DisplayName("검색어가 있으면 installation 전체에서 필터링하지만 Branch는 조회하지 않음")
    void getRepositories_success_searchAllAccessibleRepositories() {
        List<String> requestedUrls = new ArrayList<>();
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            requestedUrls.add(invocation.getArgument(1));
            return handle(invocation, ResponseEntity.ok("""
                    {
                      "repositories":[
                        {"id":101,"full_name":"seungyong/helpme.md","private":true,
                         "default_branch":"main","permissions":{"admin":true,"push":true}},
                        {"id":102,"full_name":"helpme-org/helpme-web","private":false,
                         "default_branch":"main","permissions":{"admin":false,"push":true}},
                        {"id":103,"full_name":"seungyong/unrelated","private":false,
                         "default_branch":"main","permissions":{"admin":false,"push":false}}
                      ]
                    }
                    """));
        });

        var first = githubAppAdapter.getRepositories(
                USER_ID, TOKEN, INSTALLATION_ID, "helpme", 1, 1
        );
        var second = githubAppAdapter.getRepositories(
                USER_ID, TOKEN, INSTALLATION_ID, "helpme", 2, 1
        );

        assertThat(requestedUrls).allMatch(url -> !url.contains("/branches"));
        assertThat(first.repositories()).extracting("fullName")
                .containsExactly("seungyong/helpme.md");
        assertThat(first.nextCursor()).isEqualTo("2");
        assertThat(first.hasNext()).isTrue();
        assertThat(second.repositories()).extracting("fullName")
                .containsExactly("helpme-org/helpme-web");
        assertThat(second.nextCursor()).isNull();
        assertThat(second.hasNext()).isFalse();
        verify(githubApiExecutor, atLeastOnce()).executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        );
    }

    @Test
    @DisplayName("선택한 Repository는 installation 전용 단건 API로 한 번만 조회")
    void getRepository_success_singleInstallationRequest() {
        List<String> requestedUrls = new ArrayList<>();
        given(githubApiExecutor.executeGet(
                anyLong(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            requestedUrls.add(invocation.getArgument(1));
            GithubApiExecutor.JsonResponseParser<?> parser = invocation.getArgument(3);
            return parser.parse(new ObjectMapper().readTree("""
                    {
                      "id":778899,
                      "full_name":"seungyong/helpme.md",
                      "private":true,
                      "default_branch":"main",
                      "permissions":{"admin":true,"push":true}
                    }
                    """));
        });

        var result = githubAppAdapter.getRepository(
                USER_ID, TOKEN, INSTALLATION_ID, 778899L
        );

        assertThat(requestedUrls).containsExactly(
                "https://api.github.com/user/installations/9001/repositories/778899"
        );
        assertThat(result.fullName()).isEqualTo("seungyong/helpme.md");
        assertThat(result.permissions().push()).isTrue();
        verify(githubApiExecutor, never()).executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        );
    }

    @Test
    @DisplayName("필요한 Branch가 다음 페이지에 있으면 GitHub Link를 따라 검증")
    void validateRepositoryBranches_success_pagination() {
        List<String> requestedUrls = new ArrayList<>();
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            requestedUrls.add(url);
            if (url.contains("/user/installations/")) {
                return handle(invocation, ResponseEntity.ok("{\"repositories\":[{\"id\":778899}]}"));
            }
            if (url.contains("page=2")) {
                return handle(invocation, ResponseEntity.ok("[{\"name\":\"develop\"}]"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(
                    HttpHeaders.LINK,
                    "<https://api.github.com/repos/seungyong/helpme.md/branches?per_page=100&page=2>; rel=\"next\""
            );
            return handle(
                    invocation,
                    new ResponseEntity<>("[{\"name\":\"main\"}]", headers, HttpStatus.OK)
            );
        });

        githubAppAdapter.validateRepositoryBranches(
                USER_ID,
                TOKEN,
                INSTALLATION_ID,
                778899L,
                "seungyong/helpme.md",
                Set.of("main", "develop")
        );

        assertThat(requestedUrls).containsExactly(
                "https://api.github.com/user/installations/9001/repositories?per_page=100",
                "https://api.github.com/repos/seungyong/helpme.md/branches?per_page=100",
                "https://api.github.com/repos/seungyong/helpme.md/branches?per_page=100&page=2"
        );
    }

    @Test
    @DisplayName("필요한 Branch를 모두 찾으면 다음 Branch 페이지를 조회하지 않음")
    void validateRepositoryBranches_success_stopEarly() {
        List<String> requestedUrls = new ArrayList<>();
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            requestedUrls.add(url);
            if (url.contains("/user/installations/")) {
                return handle(invocation, ResponseEntity.ok("{\"repositories\":[{\"id\":778899}]}"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(
                    HttpHeaders.LINK,
                    "<https://api.github.com/repos/seungyong/helpme.md/branches?per_page=100&page=2>; rel=\"next\""
            );
            return handle(
                    invocation,
                    new ResponseEntity<>("[{\"name\":\"main\"}]", headers, HttpStatus.OK)
            );
        });

        githubAppAdapter.validateRepositoryBranches(
                USER_ID,
                TOKEN,
                INSTALLATION_ID,
                778899L,
                "seungyong/helpme.md",
                Set.of("main")
        );

        assertThat(requestedUrls).containsExactly(
                "https://api.github.com/user/installations/9001/repositories?per_page=100",
                "https://api.github.com/repos/seungyong/helpme.md/branches?per_page=100"
        );
    }

    @Test
    @DisplayName("Repository가 installation 허용 대상이 아니면 Branch 조회 전 403")
    void validateRepositoryBranches_failure_notAccessibleToInstallation() {
        List<String> requestedUrls = new ArrayList<>();
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            requestedUrls.add(invocation.getArgument(1));
            return handle(
                    invocation,
                    ResponseEntity.ok("{\"repositories\":[{\"id\":999999}]}" )
            );
        });

        assertThatThrownBy(() -> githubAppAdapter.validateRepositoryBranches(
                USER_ID,
                TOKEN,
                INSTALLATION_ID,
                778899L,
                "seungyong/helpme.md",
                Set.of("main")
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        GithubErrorCode.GITHUB_PERMISSION_DENIED
                );

        assertThat(requestedUrls).noneMatch(url -> url.contains("/branches"));
    }

    @Test
    @DisplayName("마지막 페이지까지 필요한 Branch가 없으면 404")
    void validateRepositoryBranches_failure_branchNotFound() {
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            if (url.contains("/user/installations/")) {
                return handle(invocation, ResponseEntity.ok("{\"repositories\":[{\"id\":778899}]}"));
            }
            return handle(invocation, ResponseEntity.ok("[{\"name\":\"main\"}]"));
        });

        assertThatThrownBy(() -> githubAppAdapter.validateRepositoryBranches(
                USER_ID,
                TOKEN,
                INSTALLATION_ID,
                778899L,
                "seungyong/helpme.md",
                Set.of("main", "deleted")
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND
                );
    }

    @Test
    @DisplayName("401은 GitHub 연결 회수로 변환")
    void getInstallations_failure_unauthorized() {
        givenJsonRequestFails(new GithubApiException(HttpStatus.UNAUTHORIZED, false, 60, null));

        assertThatThrownBy(() -> githubAppAdapter.getInstallations(USER_ID, TOKEN))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        GithubErrorCode.GITHUB_CONNECTION_REVOKED
                );
    }

    @Test
    @DisplayName("Repository 403과 404를 권한·리소스 오류로 구분")
    void getRepositories_failure_permissionAndNotFound() {
        givenJsonRequestFails(new GithubApiException(HttpStatus.FORBIDDEN, false, 60, null));
        assertThatThrownBy(() -> githubAppAdapter.getRepositories(
                USER_ID, TOKEN, INSTALLATION_ID, "", 1, 30
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GithubErrorCode.GITHUB_PERMISSION_DENIED);

        reset(githubApiExecutor);
        givenJsonRequestFails(new GithubApiException(HttpStatus.NOT_FOUND, false, 60, null));
        assertThatThrownBy(() -> githubAppAdapter.getRepositories(
                USER_ID, TOKEN, INSTALLATION_ID, "", 1, 30
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("GitHub rate limit은 RATE_42901과 Retry-After 정보를 보존")
    void getRepositories_failure_rateLimit() {
        givenJsonRequestFails(new GithubApiException(HttpStatus.FORBIDDEN, true, 90, null));

        assertThatThrownBy(() -> githubAppAdapter.getRepositories(
                USER_ID, TOKEN, INSTALLATION_ID, "", 1, 30
        )).isInstanceOf(GithubRateLimitException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        GithubErrorCode.GITHUB_RATE_LIMIT_EXCEEDED
                )
                .hasFieldOrPropertyWithValue("retryAfterSeconds", 90);
    }

    @Test
    @DisplayName("필수 필드가 빠진 GitHub 응답은 502로 변환")
    void getRepositories_failure_malformedResponse() {
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> handle(invocation, ResponseEntity.ok("{\"unexpected\":[]}")));

        assertThatThrownBy(() -> githubAppAdapter.getRepositories(
                USER_ID, TOKEN, INSTALLATION_ID, "", 1, 30
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GithubErrorCode.GITHUB_UPSTREAM_ERROR);
    }

    private Object handle(
            org.mockito.invocation.InvocationOnMock invocation,
            ResponseEntity<String> response
    ) {
        Function<ResponseEntity<String>, ?> handler = invocation.getArgument(4);
        return handler.apply(response);
    }

    private void givenJsonRequestFails(GithubApiException exception) {
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willThrow(exception);
    }
}
