package seungyong.helpmebackend.repository.adapter.out.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.repository.application.port.out.command.*;
import seungyong.helpmebackend.repository.application.port.out.result.*;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RepositoryAdapterTest {
    @Mock private GithubApiExecutor githubApiExecutor;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private RepositoryAdapter repositoryAdapter;

    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    private final HttpClientErrorException notFoundException = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null
    );

    @Nested
    @DisplayName("getRepositoriesByInstallationId - 설치된 리포지토리 목록 조회")
    class GetRepositoriesByInstallationId {
        @Test
        @DisplayName("성공")
        void getRepositoriesByInstallationId_success() {
            String json = "{\"repositories\": [{\"owner\": {\"avatar_url\": \"avatar\", \"login\": \"owner\"}, \"name\": \"repo\"}], \"total_count\": 1}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<RepositoryResult> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            RepositoryResult result = repositoryAdapter.getRepositoriesByInstallationId("token", 1L, 1, 10);

            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.repositories()).hasSize(1);
            assertThat(result.repositories().get(0).getName()).isEqualTo("repo");
        }

        @Test
        @DisplayName("실패 - 리포지토리를 찾을 수 없는 경우")
        void getRepositoriesByInstallationId_failure_not_found() {
            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(4);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            assertThatThrownBy(() -> repositoryAdapter.getRepositoriesByInstallationId("token", 1L, 1, 10))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.INSTALLED_REPOSITORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getRepository - 단일 리포지토리 정보 조회")
    class GetRepository {
        @Test
        @DisplayName("성공")
        void getRepository_success() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);
            String json = "{\"owner\": {\"avatar_url\": \"url\"}, \"default_branch\": \"main\"}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<RepositoryDetailResult> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            RepositoryDetailResult result = repositoryAdapter.getRepository(command);

            assertThat(result.defaultBranch()).isEqualTo("main");
            assertThat(result.avatarUrl()).isEqualTo("url");
        }

        @Test
        @DisplayName("실패 - 접근 불가능한 리포지토리인 경우")
        void getRepository_failure_not_found() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(4);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            assertThatThrownBy(() -> repositoryAdapter.getRepository(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.REPOSITORY_CANNOT_PULL);
        }
    }

    @Nested
    @DisplayName("getContributors - 기여자 목록 조회")
    class GetContributors {
        @Test
        @DisplayName("성공 - User 타입만 필터링되어 반환")
        void getContributors_success() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);
            String json = "[{\"type\": \"User\", \"login\": \"user1\", \"avatar_url\": \"url1\"}, {\"type\": \"Bot\", \"login\": \"bot1\", \"avatar_url\": \"url2\"}]";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<List<ContributorsResult.Contributor>> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            ContributorsResult result = repositoryAdapter.getContributors(command);

            assertThat(result.contributors()).hasSize(1);
            assertThat(result.contributors().get(0).username()).isEqualTo("user1");
        }

        @Test
        @DisplayName("실패 - 리포지토리를 찾을 수 없는 경우")
        void getContributors_failure_not_found() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(4);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            assertThatThrownBy(() -> repositoryAdapter.getContributors(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getRecentSHA - 최신 SHA 조회")
    class GetRecentSHA {
        @Test
        @DisplayName("성공")
        void getRecentSHA_success() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            String json = "{\"object\": {\"sha\": \"sha-value\"}}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<String> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            String result = repositoryAdapter.getRecentSHA(command);

            assertThat(result).isEqualTo("sha-value");
        }

        @Test
        @DisplayName("실패 - 리포지토리를 찾을 수 없는 경우")
        void getRecentSHA_failure_not_found() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(4);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            assertThatThrownBy(() -> repositoryAdapter.getRecentSHA(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.BRANCH_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createBranch - 브랜치 생성")
    class CreateBranch {
        @Test
        @DisplayName("성공")
        void createBranch_success() {
            CreateBranchCommand command = fixtureMonkey.giveMeOne(CreateBranchCommand.class);

            repositoryAdapter.createBranch(command);

            verify(githubApiExecutor).executePost(anyString(), anyString(), anyMap(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("deleteBranch - 브랜치 삭제")
    class DeleteBranch {
        @Test
        @DisplayName("성공")
        void deleteBranch_success() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);

            repositoryAdapter.deleteBranch(command);

            verify(githubApiExecutor).executeDelete(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("getReadmeSHA - README SHA 조회")
    class GetReadmeSHA {
        @Test
        @DisplayName("성공")
        void getReadmeSHA_success() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            String json = "{\"sha\": \"readme-sha\"}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<String> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            String result = repositoryAdapter.getReadmeSHA(command);

            assertThat(result).isEqualTo("readme-sha");
        }

        @Test
        @DisplayName("성공 - README가 없는 경우 null 반환")
        void getReadmeSHA_success_not_found() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(4);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            String result = repositoryAdapter.getReadmeSHA(command);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("push - README 푸시")
    class Push {
        @Test
        @DisplayName("성공 - 기존 README가 있는 경우 sha 포함")
        void push_success_with_sha() {
            ReadmePushCommand command = fixtureMonkey.giveMeBuilder(ReadmePushCommand.class)
                    .set("readmeSha", "existing-sha")
                    .sample();

            repositoryAdapter.push(command);

            verify(githubApiExecutor).executePut(anyString(), anyString(), mapCaptor.capture(), anyString());
            assertThat(mapCaptor.getValue()).containsEntry("sha", "existing-sha");
        }

        @Test
        @DisplayName("성공 - 기존 README가 없는 경우 sha 미포함")
        void push_success_without_sha() {
            ReadmePushCommand command = fixtureMonkey.giveMeBuilder(ReadmePushCommand.class)
                    .set("readmeSha", null)
                    .sample();

            repositoryAdapter.push(command);

            verify(githubApiExecutor).executePut(anyString(), anyString(), mapCaptor.capture(), anyString());
            assertThat(mapCaptor.getValue()).doesNotContainKey("sha");
        }
    }

    @Nested
    @DisplayName("createPullRequest - PR 생성")
    class CreatePullRequest {
        @Test
        @DisplayName("성공")
        void createPullRequest_success() {
            CreatePullRequestCommand command = fixtureMonkey.giveMeOne(CreatePullRequestCommand.class);
            String json = "{\"html_url\": \"pr-url\"}";

            given(githubApiExecutor.executePost(anyString(), anyString(), anyMap(), any(), anyString()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<String> extractor = invocation.getArgument(3);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            String result = repositoryAdapter.createPullRequest(command);

            assertThat(result).isEqualTo("pr-url");
        }
    }

    @Nested
    @DisplayName("getRepositoryLanguages - 사용 언어 목록 조회")
    class GetRepositoryLanguages {
        @Test
        @DisplayName("성공")
        void getRepositoryLanguages_success() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);
            String json = "{\"Java\": 100, \"Python\": 200}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<List<RepositoryLanguageResult>> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            List<RepositoryLanguageResult> result = repositoryAdapter.getRepositoryLanguages(command);

            assertThat(result).hasSize(2);
            assertThat(result).extracting("name").containsExactlyInAnyOrder("Java", "Python");
        }
    }

    @Nested
    @DisplayName("getReadmeContent - README 내용 조회")
    class GetReadmeContent {
        @Test
        @DisplayName("성공")
        void getReadmeContent_success() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);

            given(githubApiExecutor.executeGetRaw(anyString(), anyString(), anyString(), any()))
                    .willReturn("content");

            String result = repositoryAdapter.getReadmeContent(command);

            assertThat(result).isEqualTo("content");
        }

        @Test
        @DisplayName("성공 - README가 없는 경우 빈 문자열 반환")
        void getReadmeContent_success_not_found() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);

            given(githubApiExecutor.executeGetRaw(anyString(), anyString(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(3);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            String result = repositoryAdapter.getReadmeContent(command);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllBranches - 모든 브랜치 조회")
    class GetAllBranches {
        @Test
        @DisplayName("성공 - 단일 페이지")
        void getAllBranches_success_single_page() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);
            ResponseEntity<String> response = ResponseEntity.ok("[{\"name\": \"main\"}]");

            given(githubApiExecutor.executeGetJson(anyString(), anyString(), any(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        Function<ResponseEntity<String>, String> extractor = invocation.getArgument(3);
                        return extractor.apply(response);
                    });

            List<String> result = repositoryAdapter.getAllBranches(command);

            assertThat(result).containsExactly("main");
        }

        @Test
        @DisplayName("성공 - 다중 페이지")
        void getAllBranches_success_multi_page() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);

            given(githubApiExecutor.executeGetJson(anyString(), anyString(), any(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        String url = invocation.getArgument(0);
                        Function<ResponseEntity<String>, String> extractor = invocation.getArgument(3);

                        if (!url.contains("page=2")) {
                            HttpHeaders headers = new HttpHeaders();
                            headers.add(HttpHeaders.LINK, "<url?page=2>; rel=\"next\"");
                            return extractor.apply(ResponseEntity.ok().headers(headers).body("[{\"name\": \"main\"}]"));
                        } else {
                            return extractor.apply(ResponseEntity.ok("[{\"name\": \"dev\"}]"));
                        }
                    });

            List<String> result = repositoryAdapter.getAllBranches(command);

            assertThat(result).containsExactly("main", "dev");
        }

        @Test
        @DisplayName("실패 - 최대 요청 횟수 초과 (무한 루프 방지)")
        void getAllBranches_failure_exceed_max_requests() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);

            given(githubApiExecutor.executeGetJson(anyString(), anyString(), any(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        Function<ResponseEntity<String>, String> extractor = invocation.getArgument(3);
                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.LINK, "<url?page=next>; rel=\"next\"");
                        return extractor.apply(ResponseEntity.ok().headers(headers).body("[{\"name\": \"main\"}]"));
                    });

            assertThatThrownBy(() -> repositoryAdapter.getAllBranches(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.GITHUB_BRANCHES_TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("실패 - 리포지토리를 찾을 수 없는 경우")
        void getAllBranches_failure_not_found() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);

            given(githubApiExecutor.executeGetJson(anyString(), anyString(), any(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(5);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            assertThatThrownBy(() -> repositoryAdapter.getAllBranches(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - JSON 파싱 오류")
        void getAllBranches_failure_json_parsing() {
            RepoInfoCommand command = fixtureMonkey.giveMeOne(RepoInfoCommand.class);
            String invalidJson = "invalid-json";

            given(githubApiExecutor.executeGetJson(anyString(), anyString(), any(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        Function<ResponseEntity<String>, String> extractor = invocation.getArgument(3);
                        return extractor.apply(ResponseEntity.ok(invalidJson));
                    });

            assertThatThrownBy(() -> repositoryAdapter.getAllBranches(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.JSON_PROCESSING_ERROR);
        }
    }

    @Nested
    @DisplayName("getRepositoryTree - 트리 구조 조회")
    class GetRepositoryTree {
        @Test
        @DisplayName("성공")
        void getRepositoryTree_success() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            String json = "{\"tree\": [{\"path\": \"file.txt\", \"type\": \"blob\"}]}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<List<RepositoryTreeResult>> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            List<RepositoryTreeResult> result = repositoryAdapter.getRepositoryTree(command);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).path()).isEqualTo("file.txt");
            assertThat(result.get(0).type()).isEqualTo("blob");
        }
    }

    @Nested
    @DisplayName("getFileContent - 파일 내용 조회")
    class GetFileContent {
        @Test
        @DisplayName("성공")
        void getFileContent_success() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            RepositoryTreeResult file = new RepositoryTreeResult("file.txt", "blob");

            given(githubApiExecutor.executeGetRaw(anyString(), anyString(), anyString(), any()))
                    .willReturn("file-content");

            RepositoryFileContentResult result = repositoryAdapter.getFileContent(command, file);

            assertThat(result.path()).isEqualTo("file.txt");
            assertThat(result.content()).isEqualTo("file-content");
        }

        @Test
        @DisplayName("실패 - 파일을 찾을 수 없는 경우 빈 문자열 반환")
        void getFileContent_failure_not_found() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            RepositoryTreeResult file = new RepositoryTreeResult("file.txt", "blob");

            given(githubApiExecutor.executeGetRaw(anyString(), anyString(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(3);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            RepositoryFileContentResult result = repositoryAdapter.getFileContent(command, file);

            assertThat(result.path()).isEqualTo("file.txt");
            assertThat(result.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkPermission - 권한 확인")
    class CheckPermission {
        @Test
        @DisplayName("성공 - admin 권한")
        void checkPermission_success_admin() {
            RepoPermissionCommand command = fixtureMonkey.giveMeOne(RepoPermissionCommand.class);
            String json = "{\"permission\": \"admin\"}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<Boolean> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            boolean result = repositoryAdapter.checkPermission(command);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공 - 권한 없음(read)")
        void checkPermission_success_read() {
            RepoPermissionCommand command = fixtureMonkey.giveMeOne(RepoPermissionCommand.class);
            String json = "{\"permission\": \"read\"}";

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.JsonResponseParser<Boolean> extractor = invocation.getArgument(2);
                        return extractor.parse(objectMapper.readTree(json));
                    });

            boolean result = repositoryAdapter.checkPermission(command);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("성공 - 404 발생 시 권한 없음으로 처리")
        void checkPermission_success_not_found() {
            RepoPermissionCommand command = fixtureMonkey.giveMeOne(RepoPermissionCommand.class);

            given(githubApiExecutor.executeGet(anyString(), anyString(), any(), anyString(), any()))
                    .willAnswer(invocation -> {
                        GithubApiExecutor.ExceptionHandler<HttpClientErrorException> errorHandler = invocation.getArgument(4);
                        return errorHandler.handle(notFoundException).orElse(null);
                    });

            boolean result = repositoryAdapter.checkPermission(command);

            assertThat(result).isFalse();
        }
    }
}