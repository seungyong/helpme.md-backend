package seungyong.helpmebackend.repository.adapter.out.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GithubRateLimitException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.repository.application.port.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.out.command.ReadmePushCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.ContributorsResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static seungyong.helpmebackend.support.fixture.TestFixtures.createBranchCommand;
import static seungyong.helpmebackend.support.fixture.TestFixtures.createPullRequestCommand;
import static seungyong.helpmebackend.support.fixture.TestFixtures.readmePushCommand;
import static seungyong.helpmebackend.support.fixture.TestFixtures.repoBranchCommand;
import static seungyong.helpmebackend.support.fixture.TestFixtures.repoInfoCommand;
import static seungyong.helpmebackend.support.fixture.TestFixtures.repoPermissionCommand;

@ExtendWith(MockitoExtension.class)
class RepositoryAdapterTest {
    @Mock private GithubApiExecutor githubApiExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RepositoryQueryAdapter repositoryQueryAdapter;
    private RepositoryContentAdapter repositoryContentAdapter;
    private RepositoryMutationAdapter repositoryMutationAdapter;

    @BeforeEach
    void setUp() {
        repositoryQueryAdapter = new RepositoryQueryAdapter(githubApiExecutor, objectMapper);
        repositoryContentAdapter = new RepositoryContentAdapter(githubApiExecutor);
        repositoryMutationAdapter = new RepositoryMutationAdapter(githubApiExecutor);
    }

    @Test
    @DisplayName("설치 저장소 목록 응답을 도메인 결과로 변환한다")
    void getRepositoriesByInstallationId_success() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willAnswer(parseJson("""
                        {"repositories":[{"owner":{"avatar_url":"avatar","login":"owner"},"name":"repo"}],"total_count":1}
                        """));

        RepositoryResult result = repositoryQueryAdapter.getRepositoriesByInstallationId(
                1L, "token", 1L, 1, 10
        );

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.repositories()).singleElement()
                .satisfies(repository -> assertThat(repository.getName()).isEqualTo("repo"));
    }

    @Test
    @DisplayName("설치 저장소 목록 404는 기존 v1 오류로 변환한다")
    void getRepositoriesByInstallationId_notFound() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willThrow(githubError(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> repositoryQueryAdapter.getRepositoriesByInstallationId(
                1L, "token", 1L, 1, 10
        ))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", RepositoryErrorCode.INSTALLED_REPOSITORY_NOT_FOUND
                );
    }

    @Test
    @DisplayName("저장소 상세 응답을 변환한다")
    void getRepository_success() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willAnswer(parseJson("{\"owner\":{\"avatar_url\":\"avatar\"},\"default_branch\":\"main\"}"));

        RepositoryDetailResult result = repositoryQueryAdapter.getRepository(repoInfoCommand());

        assertThat(result.avatarUrl()).isEqualTo("avatar");
        assertThat(result.defaultBranch()).isEqualTo("main");
    }

    @Test
    @DisplayName("저장소 조회 404는 기존 v1 도메인 오류로 변환한다")
    void getRepository_notFound() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willThrow(githubError(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> repositoryQueryAdapter.getRepository(repoInfoCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.REPOSITORY_CANNOT_PULL);
    }

    @Test
    @DisplayName("기여자 조회에서 GitHub 사용자만 유지한다")
    void getContributors_filtersBots() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willAnswer(parseJson("""
                        [
                          {"type":"User","login":"user1","avatar_url":"url1"},
                          {"type":"Bot","login":"bot1","avatar_url":"url2"}
                        ]
                        """));

        ContributorsResult result = repositoryQueryAdapter.getContributors(repoInfoCommand());

        assertThat(result.contributors()).singleElement()
                .satisfies(contributor -> assertThat(contributor.username()).isEqualTo("user1"));
    }

    @Test
    @DisplayName("저장소 언어 통계를 순서 있는 결과로 변환한다")
    void getRepositoryLanguages_success() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willAnswer(parseJson("{\"Java\":100,\"Kotlin\":50}"));

        List<RepositoryLanguageResult> result =
                repositoryQueryAdapter.getRepositoryLanguages(repoInfoCommand());

        assertThat(result).extracting(RepositoryLanguageResult::name)
                .containsExactly("Java", "Kotlin");
    }

    @Test
    @DisplayName("브랜치 목록의 next 링크를 따라가며 중복 없이 합친다")
    void getAllBranches_followsPagination() {
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            Function<ResponseEntity<String>, ?> handler = invocation.getArgument(4);
            if (!url.contains("page=2")) {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.LINK, "<https://api.github.com/branches?page=2>; rel=\"next\"");
                return handler.apply(ResponseEntity.ok().headers(headers).body("[{\"name\":\"main\"}]"));
            }
            return handler.apply(ResponseEntity.ok("[{\"name\":\"dev\"},{\"name\":\"main\"}]"));
        });

        assertThat(repositoryQueryAdapter.getAllBranches(repoInfoCommand()))
                .containsExactly("main", "dev");
    }

    @Test
    @DisplayName("비정상 next 링크가 계속되면 브랜치 요청 횟수를 제한한다")
    void getAllBranches_limitsRequests() {
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            Function<ResponseEntity<String>, ?> handler = invocation.getArgument(4);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LINK, "<https://api.github.com/branches?page=next>; rel=\"next\"");
            return handler.apply(ResponseEntity.ok().headers(headers).body("[]"));
        });

        assertThatThrownBy(() -> repositoryQueryAdapter.getAllBranches(repoInfoCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", RepositoryErrorCode.GITHUB_BRANCHES_TOO_MANY_REQUESTS
                );
    }

    @Test
    @DisplayName("브랜치 JSON 오류를 기존 파싱 오류로 변환한다")
    void getAllBranches_invalidJson() {
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            Function<ResponseEntity<String>, ?> handler = invocation.getArgument(4);
            return handler.apply(ResponseEntity.ok("invalid-json"));
        });

        assertThatThrownBy(() -> repositoryQueryAdapter.getAllBranches(repoInfoCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.JSON_PROCESSING_ERROR);
    }

    @Test
    @DisplayName("권한 조회 404는 권한 없음으로 처리한다")
    void checkPermission_notFound() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willThrow(githubError(HttpStatus.NOT_FOUND));

        assertThat(repositoryQueryAdapter.checkPermission(repoPermissionCommand())).isFalse();
    }

    @Test
    @DisplayName("admin 저장소 권한을 쓰기 가능으로 판단한다")
    void checkPermission_admin() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willAnswer(parseJson("{\"permission\":\"admin\"}"));

        assertThat(repositoryQueryAdapter.checkPermission(repoPermissionCommand())).isTrue();
    }

    @Test
    @DisplayName("GitHub rate limit 정보를 기존 도메인 예외에 보존한다")
    void query_translatesRateLimit() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willThrow(new GithubApiException(HttpStatus.FORBIDDEN, true, 23, null));

        assertThatThrownBy(() -> repositoryQueryAdapter.getRepository(repoInfoCommand()))
                .isInstanceOfSatisfying(GithubRateLimitException.class,
                        exception -> assertThat(exception.getRetryAfterSeconds()).isEqualTo(23));
    }

    @Test
    @DisplayName("최신 커밋 SHA를 콘텐츠 포트에서 조회한다")
    void getRecentSha_success() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willAnswer(parseJson("{\"object\":{\"sha\":\"sha-value\"}}"));

        assertThat(repositoryContentAdapter.getRecentSHA(repoBranchCommand())).isEqualTo("sha-value");
    }

    @Test
    @DisplayName("최신 SHA 조회 404는 브랜치 없음으로 변환한다")
    void getRecentSha_notFound() {
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willThrow(githubError(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> repositoryContentAdapter.getRecentSHA(repoBranchCommand()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.BRANCH_NOT_FOUND);
    }

    @Test
    @DisplayName("README가 없으면 SHA와 내용은 호환 가능한 빈 값으로 반환한다")
    void readme_notFoundFallbacks() {
        RepoBranchCommand command = repoBranchCommand();
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willThrow(githubError(HttpStatus.NOT_FOUND));
        given(githubApiExecutor.executeGetRaw(anyLong(), anyString(), anyString(), anyString()))
                .willThrow(githubError(HttpStatus.NOT_FOUND));

        assertThat(repositoryContentAdapter.getReadmeSHA(command)).isNull();
        assertThat(repositoryContentAdapter.getReadmeContent(command)).isEmpty();
    }

    @Test
    @DisplayName("저장소 트리와 파일 내용을 콘텐츠 포트에서 변환한다")
    void getRepositoryTreeAndFileContent_success() {
        RepoBranchCommand command = repoBranchCommand();
        given(githubApiExecutor.executeGet(anyLong(), anyString(), anyString(), any(), anyString()))
                .willAnswer(parseJson("{\"tree\":[{\"path\":\"src/App.java\",\"type\":\"blob\"}]}"));
        given(githubApiExecutor.executeGetRaw(anyLong(), anyString(), anyString(), anyString()))
                .willReturn("class App {}");

        List<RepositoryTreeResult> tree = repositoryContentAdapter.getRepositoryTree(command);
        RepositoryFileContentResult file = repositoryContentAdapter.getFileContent(command, tree.get(0));

        assertThat(tree).containsExactly(new RepositoryTreeResult("src/App.java", "blob"));
        assertThat(file).isEqualTo(new RepositoryFileContentResult("src/App.java", "class App {}"));
    }

    @Test
    @DisplayName("개별 파일 404는 경로를 보존한 빈 내용으로 반환한다")
    void getFileContent_notFound() {
        RepositoryTreeResult file = new RepositoryTreeResult("missing.txt", "blob");
        given(githubApiExecutor.executeGetRaw(anyLong(), anyString(), anyString(), anyString()))
                .willThrow(githubError(HttpStatus.NOT_FOUND));

        assertThat(repositoryContentAdapter.getFileContent(repoBranchCommand(), file))
                .isEqualTo(new RepositoryFileContentResult("missing.txt", ""));
    }

    @Test
    @DisplayName("브랜치 생성은 mutation 포트에서 GitHub 호출로 위임한다")
    void createBranch_success() {
        repositoryMutationAdapter.createBranch(createBranchCommand());

        verify(githubApiExecutor).executePost(
                anyLong(), anyString(), anyString(), anyMap(), any(), anyString()
        );
    }

    @Test
    @DisplayName("브랜치 삭제는 mutation 포트에서 GitHub 호출로 위임한다")
    void deleteBranch_success() {
        repositoryMutationAdapter.deleteBranch(repoBranchCommand());

        verify(githubApiExecutor).executeDelete(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("README push는 UTF-8 Base64 본문과 기존 SHA를 전달한다")
    void push_encodesUtf8Content() {
        ReadmePushCommand command = new ReadmePushCommand(
                repoInfoCommand(), "main", "한글 README", "existing-sha", "update README"
        );
        ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);

        repositoryMutationAdapter.push(command);

        verify(githubApiExecutor).executePut(
                anyLong(), anyString(), anyString(), bodyCaptor.capture(), anyString()
        );
        Map<String, String> body = bodyCaptor.getValue();
        assertThat(body).containsEntry("sha", "existing-sha");
        assertThat(new String(Base64.getDecoder().decode(body.get("content")), StandardCharsets.UTF_8))
                .isEqualTo("한글 README");
    }

    @Test
    @DisplayName("새 README push에서는 SHA를 생략한다")
    void push_withoutExistingSha() {
        ReadmePushCommand command = readmePushCommand(null);
        ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);

        repositoryMutationAdapter.push(command);

        verify(githubApiExecutor).executePut(
                anyLong(), anyString(), anyString(), bodyCaptor.capture(), anyString()
        );
        assertThat(bodyCaptor.getValue()).doesNotContainKey("sha");
    }

    @Test
    @DisplayName("PR 생성 응답에서 URL을 반환한다")
    void createPullRequest_success() {
        CreatePullRequestCommand command = createPullRequestCommand();
        given(githubApiExecutor.executePost(
                anyLong(), anyString(), anyString(), anyMap(), any(), anyString()
        )).willAnswer(parseJsonAtArgument("{\"html_url\":\"https://github.com/pr/1\"}", 4));

        assertThat(repositoryMutationAdapter.createPullRequest(command))
                .isEqualTo("https://github.com/pr/1");
    }

    private Answer<Object> parseJson(String json) {
        return parseJsonAtArgument(json, 3);
    }

    private Answer<Object> parseJsonAtArgument(String json, int parserArgument) {
        return invocation -> {
            GithubApiExecutor.JsonResponseParser<?> parser = invocation.getArgument(parserArgument);
            return parser.parse(objectMapper.readTree(json));
        };
    }

    private GithubApiException githubError(HttpStatus status) {
        return new GithubApiException(status, false, 60, null);
    }
}
