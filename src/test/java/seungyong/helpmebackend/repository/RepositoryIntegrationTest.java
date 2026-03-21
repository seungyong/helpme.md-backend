package seungyong.helpmebackend.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.domain.type.RedisKeyFactory;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.global.infrastructure.github.GithubClient;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.project.adapter.out.persistence.ProjectAdapter;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestDraftEvaluation;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestGeneration;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestPull;
import seungyong.helpmebackend.repository.adapter.in.web.dto.response.ResponseEvaluation;
import seungyong.helpmebackend.repository.adapter.out.gpt.GPTClient;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.EvaluationCommand;
import seungyong.helpmebackend.repository.application.port.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.EvaluationContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;
import seungyong.helpmebackend.section.adapter.out.persistence.SectionAdapter;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.sse.application.port.out.SSEPortOut;
import seungyong.helpmebackend.sse.domain.type.SSETaskName;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
public class RepositoryIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProjectAdapter projectAdapter;
    @Autowired private SectionAdapter sectionAdapter;

    @MockitoSpyBean private UserPortOut userPortOut;
    @MockitoBean private GithubClient githubClient;
    @MockitoSpyBean private GithubApiExecutor githubApiExecutor;
    @MockitoBean private GPTClient gptClient;
    @MockitoSpyBean private CipherPortOut  cipherPortOut;
    @MockitoSpyBean private SSEPortOut ssePortOut;
    @MockitoSpyBean private RedisPortOut redisPortOut;
    @MockitoSpyBean private SectionPortOut sectionPortOut;

    private User user;
    private JWT jwt;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    private final HttpClientErrorException notFoundException = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null
    );

    @BeforeEach
    void setup() {
        User newUser = new User(
                null,
                new GithubUser(
                        "test-user",
                        123456L,
                        new EncryptedToken("encrypted-token")
                )
        );
        user = userPortOut.save(newUser);

        jwt = jwtProvider.generate(new JWTUser(user.getId(), "test-user"));
        lenient().when(cipherPortOut.decrypt(any(String.class))).thenReturn("decrypted-token");
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void cleanup() {
        userPortOut.delete(user);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Nested
    class GetRepositories{
        @Test
        @DisplayName("성공")
        void getRepository_success() throws Exception {
            String installationId = "12345678";
            String json = "{\"repositories\": [{\"owner\": {\"avatar_url\": \"avatar\", \"login\": \"owner\"}, \"name\": \"repo\"}], \"total_count\": 1}";

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willReturn(json);
            mockMvc.perform(get("/api/v1/repos?installation_id=" + installationId)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.repositories").isArray());
        }

        @Test
        @DisplayName("성공 - 빈 레포지토리 목록")
        void getRepository_empty() throws Exception {
            String installationId = "12345678";
            String json = "{\"repositories\": [], \"total_count\": 0}";

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willReturn(json);

            mockMvc.perform(get("/api/v1/repos?installation_id=" + installationId)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.repositories").isArray());
        }

        @Test
        @DisplayName("실패 - 설치 ID에 해당하는 레포지토리가 없는 경우")
        void getRepository_notFound() throws Exception {
            String installationId = "9999";

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willThrow(notFoundException);

            mockMvc.perform(get("/api/v1/repos?installation_id=" + installationId)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.INSTALLED_REPOSITORY_NOT_FOUND.getErrorCode()));
        }
    }

    @Nested
    class GetRepository {
        @Test
        @DisplayName("성공")
        void getRepository_success() throws Exception {
            String owner = "test-owner";
            String name = "test-repo";
            String json = """
                    {
                        "owner": {
                            "avatar_url": "avatar",
                            "login": "owner"
                        },
                        "default_branch": "main"
                    }
                    """;

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willReturn(json);

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}", owner, name)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(name))
                    .andExpect(jsonPath("$.owner").value(owner))
                    .andExpect(jsonPath("$.avatarUrl").value("avatar"))
                    .andExpect(jsonPath("$.defaultBranch").value("main"));
        }

        @Test
        @DisplayName("실패 - 레포지토리가 없는 경우")
        void getRepository_notFound() throws Exception {
            String owner = "nonexistent-owner";
            String name = "nonexistent-repo";

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willThrow(notFoundException);

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}", owner, name)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.REPOSITORY_CANNOT_PULL.getErrorCode()));
        }
    }

    @Nested
    class GetBranches {
        @Test
        @DisplayName("성공")
        void getBranches_success() throws Exception {
            String owner = "test-owner";
            String name = "test-repo";
            String mockResponseBody = "[{\"name\": \"main\"}, {\"name\": \"feature\"}]";
            ResponseEntity<String> mockResponse = ResponseEntity.ok(mockResponseBody);

            doAnswer(invocation -> {
                        Function<ResponseEntity<String>, String> extractor = invocation.getArgument(3);
                        return extractor.apply(mockResponse);
                    })
                    .when(githubApiExecutor).executeGetJson(anyString(), anyString(), anyString(), any(), anyString(), any());

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/branches", owner, name)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.branches").isArray());
        }

        @Test
        @DisplayName("실패 - 10회 요청 초과")
        void getBranches_failure_maxRequests() throws Exception {
            String owner = "test-owner";
            String name = "test-repo";

            // Link 헤더에 다음 페이지(next)가 항상 있다고 설정하여 무한 루프 유도
            HttpHeaders headers = new HttpHeaders();
            headers.add("Link", "<https://api.github.com/next-url>; rel=\"next\"");

            ResponseEntity<String> mockResponse = new ResponseEntity<>("[{\"name\": \"branch\"}]", headers, org.springframework.http.HttpStatus.OK);

            doAnswer(invocation -> {
                Function<ResponseEntity<String>, Object> extractor = invocation.getArgument(3);
                return extractor.apply(mockResponse);
            }).when(githubApiExecutor).executeGetJson(anyString(), anyString(), any(), any(), anyString(), any());

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/branches", owner, name)
                            .cookie(new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("실패 - 레포지토리가 없는 경우")
        void getBranches_notFound() throws Exception {
            String owner = "nonexistent-owner";
            String name = "nonexistent-repo";

            given(githubClient.fetchGet(anyString(), anyString(), anyString(), any())).willThrow(notFoundException);

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/branches", owner, name)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND.getErrorCode()));
        }

        @Test
        @DisplayName("실패 - JSON 파싱")
        void getBranches_failure_jsonParsing() throws Exception {
            String owner = "test-owner";
            String name = "test-repo";
            String invalidJson = "invalid json";

            given(githubClient.fetchGetMethodForBody(anyString(), anyString())).willReturn(invalidJson);

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/branches", owner, name)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.JSON_PROCESSING_ERROR.getErrorCode()));
        }
    }

    @Nested
    class GetFallback {
        @Test
        @DisplayName("성공 - 임시 저장된 README 초안")
        void getFallbackDraftEvaluation_success() throws Exception {
            String taskId = "task-123";
            String redisKey = RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + taskId;
            ResponseEvaluation evaluation = fixtureMonkey.giveMeOne(ResponseEvaluation.class);
            redisPortOut.setObject(redisKey, evaluation, Instant.now().plus(5, ChronoUnit.MINUTES));

            mockMvc.perform(get("/api/v1/repos/fallback/evaluate/draft/{taskId}", taskId)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(evaluation.rating()));

            verify(redisPortOut, times(1)).getObject(eq(redisKey), any());
            verify(redisPortOut, times(1)).delete(eq(redisKey));
            verify(ssePortOut, times(1)).deleteEmitter(anyString());
        }

        @Test
        @DisplayName("실패 - 임시 저장된 README 초안이 없는 경우")
        void getFallbackDraftEvaluation_notFound() throws Exception {
            String taskId = "nonexistent-task";
            String redisKey = RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + taskId;

            mockMvc.perform(get("/api/v1/repos/fallback/evaluate/draft/{taskId}", taskId)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.FALLBACK_NOT_FOUND.getErrorCode()));

            verify(redisPortOut, times(1)).getObject(eq(redisKey), any());
            verify(redisPortOut, never()).delete(anyString());
            verify(ssePortOut, never()).deleteEmitter(anyString());
        }

        @Test
        @DisplayName("성공 - 임시 저장된 README 초안 내용")
        void getFallbackGenerate_success() throws Exception {
            String taskId = "task-123";
            String redisKey = RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + taskId;

            ResponseSections sections = fixtureMonkey.giveMeBuilder(ResponseSections.class)
                    .size("sections", 3)
                    .sample();
            redisPortOut.setObject(redisKey, sections, Instant.now().plus(5, ChronoUnit.MINUTES));

            mockMvc.perform(get("/api/v1/repos/fallback/generate/{taskId}", taskId)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sections").isArray())
                    .andExpect(jsonPath("$.sections[0].title").value(sections.sections().get(0).title()));

            verify(redisPortOut, times(1)).getObject(eq(redisKey), any());
            verify(redisPortOut, times(1)).delete(eq(redisKey));
            verify(ssePortOut, times(1)).deleteEmitter(anyString());
        }

        @Test
        @DisplayName("실패 - 임시 저장된 README 초안 내용이 없는 경우")
        void getFallbackGenerate_notFound() throws Exception {
            String taskId = "nonexistent-task";
            String redisKey = RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + taskId;

            mockMvc.perform(get("/api/v1/repos/fallback/generate/{taskId}", taskId)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.FALLBACK_NOT_FOUND.getErrorCode()));

            verify(redisPortOut, times(1)).getObject(eq(redisKey), any());
            verify(redisPortOut, never()).delete(anyString());
            verify(ssePortOut, never()).deleteEmitter(anyString());
        }
    }

    @Nested
    class PR {
        private final String branch = "main";
        private final String owner = "test-owner";
        private final String name = "test-repo";
        private final String sha = "abc123";

        @Test
        @DisplayName("성공")
        void pr_success() throws Exception {
            RequestPull request = new RequestPull(branch, "Updated README content");
            String prUrl = "https://api.github.com/repos/test-owner/test-repo/pulls/1";
            String recentJson = """
                    {
                        "object": {
                            "sha": "%s"
                        }
                    }
                    """.formatted(sha);

            given(githubClient.fetchGetMethodForBody(eq(String.format(
                    "https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                    owner,
                    name,
                    branch
            )), anyString())).willReturn(recentJson);

            String branchJson = """
                    {
                        "ref": "%s"
                    }
                    """.formatted(branch);

            given(githubClient.postWithBearer(eq(String.format(
                    "https://api.github.com/repos/%s/%s/git/refs",
                    owner,
                    name
            )), anyString(), anyMap(), any())).willReturn(branchJson);

            given(githubClient.fetchGetMethodForBody(eq(String.format(
                    "https://api.github.com/repos/%s/%s/contents/README.md?ref=%s",
                    owner,
                    name,
                    branch
            )), anyString())).willReturn("{\"sha\": \"%s\"}".formatted(sha));

            doNothing().when(githubApiExecutor).executePut(eq(String.format(
                    "https://api.github.com/repos/%s/%s/contents/%s",
                    owner,
                    name,
                    "README.md"
            )), anyString(), anyMap(), anyString());

            String prUrlJson = """
                    {
                        "html_url": "%s"
                    }
                    """.formatted(prUrl);
            given(githubClient.postWithBearer(eq(String.format(
                    "https://api.github.com/repos/%s/%s/pulls",
                    owner,
                    name
            )), anyString(), anyMap(), any())).willReturn(prUrlJson);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}", owner, name)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.htmlUrl").value(prUrl));
        }

        @Test
        @DisplayName("성공 - README 파일이 없는 경우")
        void pr_success_noReadme() throws Exception {
            String prUrl = "https://api.github.com/repos/test-owner/test-repo/pulls/1";
            RequestPull request = new RequestPull(branch, "Updated README content");

            // 1. 최근 SHA 조회
            String recentJson = "{ \"object\": { \"sha\": \"%s\" } }".formatted(sha);
            given(githubClient.fetchGetMethodForBody(contains("/git/refs/heads/"), anyString()))
                    .willReturn(recentJson);

            // 2. 새 브랜치 생성
            String branchJson = "{ \"ref\": \"%s\" }".formatted(branch);
            given(githubClient.postWithBearer(contains("/git/refs"), anyString(), anyMap(), any()))
                    .willReturn(branchJson);

            // 3. README 조회 - 404 Not Found 예외 발생
            given(githubClient.fetchGetMethodForBody(contains("/contents/README.md"), anyString()))
                    .willThrow(notFoundException);

            // 4. Push 및 PR 생성
            doNothing().when(githubApiExecutor).executePut(anyString(), anyString(), anyMap(), anyString());

            String prUrlJson = "{ \"html_url\": \"%s\" }".formatted(prUrl);
            given(githubClient.postWithBearer(contains("/pulls"), anyString(), anyMap(), any()))
                    .willReturn(prUrlJson);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}", owner, name)
                            .cookie(new Cookie("accessToken", jwt.getAccessToken()), new Cookie("refreshToken", jwt.getRefreshToken()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.htmlUrl").value(prUrl));
        }

        @Test
        @DisplayName("실패 - 브랜치 없는 경우")
        void pr_branchNotFound() throws Exception {
            RequestPull request = new RequestPull(branch, "Updated README content");

            // 첫 번째 호출(SHA 조회)에서 바로 에러 발생 시뮬레이션
            given(githubClient.fetchGetMethodForBody(anyString(), anyString()))
                    .willThrow(notFoundException);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}", owner, name)
                            .cookie(new Cookie("accessToken", jwt.getAccessToken()), new Cookie("refreshToken", jwt.getRefreshToken()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.BRANCH_NOT_FOUND.getErrorCode()));
        }

        @Test
        @DisplayName("실패 - Push")
        void pr_pushFailure() throws Exception {
            RequestPull request = new RequestPull(branch, "Updated README content");

            // 정상 응답 설정 (SHA 조회, 브랜치 생성, README 조회)
            given(githubClient.fetchGetMethodForBody(contains("/git/refs/heads/"), anyString())).willReturn("{\"object\":{\"sha\":\""+sha+"\"}}");
            given(githubClient.postWithBearer(contains("/git/refs"), anyString(), anyMap(), any())).willReturn("{\"ref\":\""+branch+"\"}");
            given(githubClient.fetchGetMethodForBody(contains("/contents/README.md"), anyString())).willReturn("{\"sha\":\""+sha+"\"}");

            // Push 시 에러 발생
            doThrow(new RuntimeException("Push failed"))
                    .when(githubApiExecutor).executePut(anyString(), anyString(), anyMap(), anyString());

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}", owner, name)
                            .cookie(new Cookie("accessToken", jwt.getAccessToken()), new Cookie("refreshToken", jwt.getRefreshToken()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.PUSH_FAILED.getErrorCode()));

            // 브랜치 삭제(Cleanup) 호출 확인
            verify(githubApiExecutor).executeDelete(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - PR 생성")
        void pr_creationFailure() throws Exception {
            RequestPull request = new RequestPull(branch, "Updated README content");

            // PR 생성 전까지는 모두 성공
            given(githubClient.fetchGetMethodForBody(contains("/git/refs/heads/"), anyString())).willReturn("{\"object\":{\"sha\":\""+sha+"\"}}");
            given(githubClient.postWithBearer(contains("/git/refs"), anyString(), anyMap(), any())).willReturn("{\"ref\":\""+branch+"\"}");
            given(githubClient.fetchGetMethodForBody(contains("/contents/README.md"), anyString())).willReturn("{\"sha\":\""+sha+"\"}");
            doNothing().when(githubApiExecutor).executePut(anyString(), anyString(), anyMap(), anyString());

            // PR 생성(Post) 시 에러 발생
            given(githubClient.postWithBearer(contains("/pulls"), anyString(), anyMap(), any()))
                    .willThrow(new RuntimeException("PR creation failed"));

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}", owner, name)
                            .cookie(new Cookie("accessToken", jwt.getAccessToken()), new Cookie("refreshToken", jwt.getRefreshToken()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(RepositoryErrorCode.PR_CREATION_FAILED.getErrorCode()));

            verify(githubApiExecutor).executeDelete(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @Transactional(propagation = Propagation.NEVER)
    class SSE {
        private final String owner = "test-owner";
        private final String name = "test-repo";
        private final String sha = "abc123";

        private String extractTaskId(String body) throws Exception {
            ObjectMapper objectMapper = new ObjectMapper();

            // 1. "data:" 이후의 JSON 문자열만 추출
            String jsonPart = body.substring(body.indexOf("data:") + 5).trim();

            // 2. JSON 파싱 후 taskId 값 읽기
            JsonNode node = objectMapper.readTree(jsonPart);
            return node.get("taskId").asText();
        }

        private String createCommitsJson(String prefix) {
            ArrayList<String> commits = new ArrayList<>();

            for (int i = 0; i < 40; i++) {
                String commitJson = """
                    {
                        "sha": "%s-%02d",
                        "commit": {
                            "message": "Commit message %02d",
                            "committer": {
                                "date": "2024-01-%02dT12:00:00Z"
                            }
                        }
                    }
                    """.formatted(prefix, i + 1, i + 1, (i / 10) + 1);

                commits.add(commitJson);
            }

            return "[" + String.join(", ", commits) + "]";
        }

        private void verifyCaches(String owner, String name, String sha) {
            String readmeCacheKey = RedisKeyFactory.createReadmeKey(owner, name, sha);
            String commitCacheKey = RedisKeyFactory.createCommitsKey(owner, name, sha);
            String langCacheKey = RedisKeyFactory.createLanguageKey(owner, name, sha);
            String treeCacheKey = RedisKeyFactory.createTreeKey(owner, name, sha);
            String repoInfoCacheKey = RedisKeyFactory.createRepoInfoKey(owner, name, sha);
            String entryCacheKey = RedisKeyFactory.createEntryFileKey(owner, name, sha);
            String importantCacheKey = RedisKeyFactory.createImportanceFileKey(owner, name, sha);

            // 캐시 저장 확인
            assertThat(redisPortOut.exists(readmeCacheKey)).isTrue();
            assertThat(redisPortOut.exists(commitCacheKey)).isTrue();
            assertThat(redisPortOut.exists(langCacheKey)).isTrue();
            assertThat(redisPortOut.exists(treeCacheKey)).isTrue();
            assertThat(redisPortOut.exists(repoInfoCacheKey)).isTrue();
            assertThat(redisPortOut.exists(entryCacheKey)).isTrue();
            assertThat(redisPortOut.exists(importantCacheKey)).isTrue();

            // cache hit redis set 안 된 거 확인
            verify(redisPortOut, times(1)).get(readmeCacheKey);

            // cache 저장
            verify(redisPortOut, times(1)).setObject(eq(commitCacheKey), any(), any());
            verify(redisPortOut, times(1)).setObject(eq(langCacheKey), any(), any());
            verify(redisPortOut, times(1)).setObject(eq(treeCacheKey), any(), any());
            verify(redisPortOut, times(1)).setObject(eq(repoInfoCacheKey), any(), any());
            verify(redisPortOut, times(1)).set(eq(entryCacheKey), anyString(), any());
            verify(redisPortOut, times(1)).set(eq(importantCacheKey), anyString(), any());
        }

        private void mockGithubBaseFlow(String branch) throws Exception {
            // SHA 조회
            given(githubClient.fetchGetMethodForBody(contains("/git/refs/heads/" + branch), anyString()))
                    .willReturn("{\"object\":{\"sha\":\"%s\"}}".formatted(sha));

            // README 캐시 히트 설정
            String readmeCacheKey = RedisKeyFactory.createReadmeKey(owner, name, sha);
            redisPortOut.set(readmeCacheKey, "README content", Instant.now().plus(5, ChronoUnit.MINUTES));

            // Contributors & Commits
            given(githubClient.fetchGetMethodForBody(contains("/contributors"), anyString()))
                    .willReturn("[{\"type\": \"User\", \"login\": \"user1\", \"avatar_url\": \"url1\"}]");

            HttpHeaders commitHeaders = new HttpHeaders();
            commitHeaders.set(HttpHeaders.LINK, "<https://api.github.com/repositories/123/commits?page=3>; rel=\"last\"");
            given(githubClient.fetchGet(contains("/commits"), anyString(), anyString(), eq(String.class)))
                    .willAnswer(invocation -> ResponseEntity.ok().headers(commitHeaders).body(createCommitsJson("commit")));

            // Languages, Trees, Contents
            given(githubClient.fetchGetMethodForBody(contains("/languages"), any()))
                    .willReturn("{\"Java\": 70, \"Python\": 30}");
            given(githubClient.fetchGetMethodForBody(contains("/git/trees"), anyString()))
                    .willReturn("{\"tree\": [{\"path\": \"file.txt\", \"type\": \"blob\"}]}");
            given(githubClient.fetchGetMethodForBody(contains("/contents/"), anyString(), anyString()))
                    .willReturn("{\"content\": \"R0lGODlhAQABAIAAAAUEBAAAACwAAAAAAQABAAACAkQBADs=\"}");
        }

        private String subscribeAndGetTaskId() throws Exception {
            MvcResult mvcResult = mockMvc.perform(get("/api/v1/sse/subscribe").accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.request().asyncStarted())
                    .andReturn();
            return extractTaskId(mvcResult.getResponse().getContentAsString());
        }

        @Nested
        class Evaluation {
            RequestDraftEvaluation request = new RequestDraftEvaluation("main", "Draft README content");

            @Test
            @DisplayName("성공")
            void evaluateDraftReadme_success() throws Exception {
                mockGithubBaseFlow(request.branch());

                // GPT 응답 설정
                GPTRepositoryInfoResult repoInfo = fixtureMonkey.giveMeOne(GPTRepositoryInfoResult.class);
                EvaluationContentResult evaluation = fixtureMonkey.giveMeOne(EvaluationContentResult.class);

                given(gptClient.getRepositoryInfo(anyString(), any(RepositoryInfoCommand.class)))
                        .willReturn(repoInfo);
                given(gptClient.evaluateReadme(any(EvaluationCommand.class)))
                        .willReturn(evaluation);

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/evaluate/draft/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                // 5초 동안 Async 비동기 스레드 내에서 해당 메소드 호출이 일어나면 바로 나옴
                // 바로 확인하면 Redis 캐싱 처리 등이 처리되기도 전에 체크를 해버림
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_EVALUATE_DRAFT.getTaskName()),
                            any(ResponseEvaluation.class)
                    );
                });

                verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any());
                verifyCaches(owner, name, sha);
            }

            @Test
            @DisplayName("실패 - 유저가 없는 경우")
            void evaluateDraftReadme_failure_noUser() throws Exception {
                // 유저 삭제
                userPortOut.delete(user);

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/evaluate/draft/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_EVALUATE_DRAFT_ERROR.getTaskName()),
                            any(ResponseEntity.class)
                    );
                });

                // 단순 오류인 경우 폴백 저장을 하지 않음
                verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any(Instant.class));
            }

            @Test
            @DisplayName("실패 - 커밋이 없는 경우")
            void evaluateDraftReadme_success_noCommits() throws Exception {
                given(githubClient.fetchGetMethodForBody(eq(String.format(
                        "https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                        owner,
                        name,
                        request.branch()
                )), anyString())).willThrow(notFoundException);

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/evaluate/draft/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_EVALUATE_DRAFT_ERROR.getTaskName()),
                            any(ResponseEntity.class)
                    );
                });

                // 단순 오류인 경우 폴백 저장을 하지 않음
                verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any(Instant.class));
            }

            @Test
            @DisplayName("실패 - GPT 평가 실패")
            void evaluateDraftReadme_failure_gptEvaluation() throws Exception {
                mockGithubBaseFlow(request.branch());

                given(gptClient.getRepositoryInfo(anyString(), any(RepositoryInfoCommand.class)))
                        .willThrow(new RuntimeException("GPT Repository Info Error"));

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/evaluate/draft/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_EVALUATE_DRAFT_ERROR.getTaskName()),
                            any(ResponseEntity.class)
                    );
                });

                verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any(Instant.class));
            }

            @Test
            @DisplayName("실패 - 성공 응답 실패 (폴백 저장)")
            void evaluateDraftReadme_failure_sendCompletion() throws Exception {
                mockGithubBaseFlow(request.branch());

                // 5. GPT 분석 & 7. GPT 평가
                GPTRepositoryInfoResult repoInfo = fixtureMonkey.giveMeOne(GPTRepositoryInfoResult.class);
                EvaluationContentResult evaluation = fixtureMonkey.giveMeOne(EvaluationContentResult.class);

                given(gptClient.getRepositoryInfo(anyString(), any(RepositoryInfoCommand.class)))
                        .willReturn(repoInfo);
                given(gptClient.evaluateReadme(any(EvaluationCommand.class)))
                        .willReturn(evaluation);

                given(ssePortOut.sendCompletion(anyString(), eq(SSETaskName.COMPLETION_EVALUATE_DRAFT.getTaskName()), any(ResponseEvaluation.class)))
                        .willReturn(false); // 전송 실패 시뮬레이션

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/evaluate/draft/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                // 폴백 저장 검증
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, timeout(5000)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_EVALUATE_DRAFT.getTaskName()),
                            any(ResponseEvaluation.class)
                    );
                });

                verify(redisPortOut, times(1)).setObjectIfAbsent(
                        eq(RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + taskId),
                        any(ResponseEvaluation.class),
                        any(Instant.class)
                );
            }
        }

        @Nested
        class Generation {
            private final String generatedContent = """
                    # Project Title
                    ## Overview
                    This project is designed to...
                    - Key feature 1
                    
                    ## Installation
                    To install the project, follow these steps:
                    1. Step one
                    
                    ## Usage
                    Here's how to use the project:
                    ```bash
                    command to run the project
                    ```
                    """;
            RequestGeneration request = new RequestGeneration("main");

            @Captor
            ArgumentCaptor<ResponseSections> captor;

            @Test
            @DisplayName("성공")
            void generation() throws Exception {
                mockGithubBaseFlow(request.branch());

                GPTRepositoryInfoResult repoInfo = fixtureMonkey.giveMeOne(GPTRepositoryInfoResult.class);
                EvaluationContentResult evaluation = fixtureMonkey.giveMeOne(EvaluationContentResult.class);

                given(gptClient.getRepositoryInfo(anyString(), any(RepositoryInfoCommand.class)))
                        .willReturn(repoInfo);
                given(gptClient.evaluateReadme(any(EvaluationCommand.class)))
                        .willReturn(evaluation);

                // GPT README 초안 생성
                given(gptClient.generateDraftReadme(any(GenerateReadmeCommand.class)))
                        .willReturn(generatedContent);

                String taskId = subscribeAndGetTaskId();

                MvcResult result = mockMvc.perform(post("/api/v1/repos/{owner}/{name}/generate/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted())
                        .andReturn();

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_GENERATE.getTaskName()),
                            captor.capture()
                    );
                });

                ResponseSections responseSections = captor.getValue();

                assertThat(responseSections.sections())
                        .isNotEmpty()
                        .hasSize(4)
                        .extracting(ResponseSections.Section::title)
                        .containsExactly("Project Title", "Overview", "Installation", "Usage");

                verifyCaches(owner, name, sha);

                assertThat(projectAdapter.getByUserIdAndRepoFullName(user.getId(), owner + "/" + name))
                        .isPresent();

                assertThat(sectionAdapter.getSectionsByUserIdAndRepoFullName(user.getId(), owner + "/" + name))
                        .isNotEmpty()
                        .hasSize(4);

                verify(sectionPortOut, never()).deleteAllByUserIdAndRepoFullName(anyLong(), anyString());
            }

            @Test
            @DisplayName("실패 - 유저가 없는 경우")
            void generation_failure_noUser() throws Exception {
                // 유저 삭제
                userPortOut.delete(user);

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/generate/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_GENERATE_ERROR.getTaskName()),
                            any(ResponseEntity.class)
                    );
                });

                // 단순 오류인 경우 폴백 저장을 하지 않음
                verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any(Instant.class));
            }

            @Test
            @DisplayName("실패 - 커밋이 없는 경우")
            void generation_failure_noCommits() throws Exception {
                given(githubClient.fetchGetMethodForBody(eq(String.format(
                        "https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                        owner,
                        name,
                        request.branch()
                )), anyString())).willThrow(notFoundException);

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/generate/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_GENERATE_ERROR.getTaskName()),
                            any(ResponseEntity.class)
                    );
                });

                // 단순 오류인 경우 폴백 저장을 하지 않음
                verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any(Instant.class));
            }

            @Test
            @DisplayName("실패 - GPT 생성 실패")
            void generation_failure_gptGeneration() throws Exception {
                mockGithubBaseFlow(request.branch());

                given(gptClient.getRepositoryInfo(anyString(), any(RepositoryInfoCommand.class)))
                        .willThrow(new RuntimeException("GPT Repository Info Error"));

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/generate/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, times(1)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_GENERATE_ERROR.getTaskName()),
                            any(ResponseEntity.class)
                    );
                });

                verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any(Instant.class));
            }

            @Test
            @DisplayName("실패 - 성공 응답 실패 (폴백 저장)")
            void generation_failure_sendCompletion() throws Exception {
                mockGithubBaseFlow(request.branch());

                GPTRepositoryInfoResult repoInfo = fixtureMonkey.giveMeOne(GPTRepositoryInfoResult.class);
                EvaluationContentResult evaluation = fixtureMonkey.giveMeOne(EvaluationContentResult.class);

                given(gptClient.getRepositoryInfo(anyString(), any(RepositoryInfoCommand.class)))
                        .willReturn(repoInfo);
                given(gptClient.evaluateReadme(any(EvaluationCommand.class)))
                        .willReturn(evaluation);

                given(gptClient.generateDraftReadme(any(GenerateReadmeCommand.class)))
                        .willReturn(generatedContent);

                given(ssePortOut.sendCompletion(anyString(), eq(SSETaskName.COMPLETION_GENERATE.getTaskName()), any(ResponseSections.class)))
                        .willReturn(false); // 전송 실패 시뮬레이션

                String taskId = subscribeAndGetTaskId();

                mockMvc.perform(post("/api/v1/repos/{owner}/{name}/generate/sse", owner, name)
                                .param("taskId", taskId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType("application/json")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                ))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isAccepted());

                // 폴백 저장 검증
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    verify(ssePortOut, timeout(5000)).sendCompletion(
                            eq(taskId),
                            eq(SSETaskName.COMPLETION_GENERATE.getTaskName()),
                            any(ResponseSections.class)
                    );
                });

                verify(redisPortOut, times(1)).setObjectIfAbsent(
                        eq(RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + taskId),
                        any(ResponseSections.class),
                        any(Instant.class)
                );
            }
        }
    }
}