package seungyong.helpmebackend.repository.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import net.jqwik.api.Arbitraries;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestDraftEvaluation;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestGeneration;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestPull;
import seungyong.helpmebackend.repository.adapter.in.web.dto.response.*;
import seungyong.helpmebackend.repository.application.port.out.*;
import seungyong.helpmebackend.repository.application.port.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.out.command.ReadmePushCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.*;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.sse.application.port.out.SSEPortOut;
import seungyong.helpmebackend.sse.domain.type.SSETaskName;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {
    @Mock private UserPortOut userPortOut;
    @Mock private RepositoryPortOut repositoryPortOut;
    @Mock private CipherPortOut cipherPortOut;
    @Mock private ObjectCipherPortOut objectCipherPortOut;
    @Mock private GPTPortOut gptPortOut;
    @Mock private RedisPortOut redisPortOut;
    @Mock private RepositoryTreeFilterPortOut repositoryTreeFilterPortOut;
    @Mock private SSEPortOut ssePortOut;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private SectionPortOut sectionPortOut;
    @Mock private CommitPortOut commitPortOut;

    @InjectMocks private RepositoryService repositoryService;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    private final Long USER_ID = 1L;
    private final String OWNER = "owner";
    private final String NAME = "repo";
    private final String ACCESS_TOKEN = "decrypted-token";
    private final String ENCRYPTED_TOKEN = "encrypted-token";

    @BeforeEach
    void setUp() {
        User mockUser = mock(User.class, RETURNS_DEEP_STUBS);

        lenient().when(mockUser.getGithubUser().getGithubToken().value()).thenReturn(ENCRYPTED_TOKEN);
        lenient().when(userPortOut.getById(USER_ID)).thenReturn(mockUser);
        lenient().when(cipherPortOut.decrypt(ENCRYPTED_TOKEN)).thenReturn(ACCESS_TOKEN);
    }

    @Nested
    class GetRepositories {
        @Test
        @DisplayName("성공")
        void getRepositories_success() {
            RepositoryResult repos = fixtureMonkey.giveMeBuilder(RepositoryResult.class)
                    .set("totalCount", Arbitraries.integers().greaterOrEqual(1))
                    .size("repositories", 3)
                    .sample();

            given(repositoryPortOut.getRepositoriesByInstallationId(anyString(), anyLong(), anyInt(), anyInt()))
                    .willReturn(repos);

            ResponseRepositories response = repositoryService.getRepositories(USER_ID, 1L, 1, 10);

            assertThat(response.totalCount()).isGreaterThan(1);
            assertThat(response.repositories()).hasSize(3);
        }

        @Test
        @DisplayName("성공 - 레포지토리가 없는 경우")
        void getRepositories_success_empty() {
            RepositoryResult repos = new RepositoryResult(Collections.emptyList(), 0);

            given(repositoryPortOut.getRepositoriesByInstallationId(anyString(), anyLong(), anyInt(), anyInt()))
                    .willReturn(repos);

            ResponseRepositories response = repositoryService.getRepositories(USER_ID, 1L, 1, 10);

            assertThat(response.totalCount()).isZero();
            assertThat(response.repositories()).isEmpty();
        }
    }

    @Nested
    class GetRepository {
        @Test
        @DisplayName("성공")
        void getRepository_success() {
            RepositoryDetailResult repoDetail = fixtureMonkey.giveMeBuilder(RepositoryDetailResult.class)
                    .set("owner", OWNER)
                    .set("name", NAME)
                    .sample();

            given(repositoryPortOut.getRepository(any(RepoInfoCommand.class)))
                    .willReturn(repoDetail);

            ResponseRepository response = repositoryService.getRepository(USER_ID, OWNER, NAME);

            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.owner()).isEqualTo(OWNER);
                        assertThat(r.name()).isEqualTo(NAME);
                    });
        }
    }

    @Nested
    class GetBranches {
        @Test
        @DisplayName("성공")
        void getBranches_success() {
            given(repositoryPortOut.getAllBranches(any(RepoInfoCommand.class)))
                    .willReturn(List.of("main", "develop", "feature"));

            ResponseBranches response = repositoryService.getBranches(USER_ID, OWNER, NAME);

            assertThat(response.branches()).hasSize(3)
                    .containsExactlyInAnyOrder("main", "develop", "feature");
        }
    }

    @Nested
    class getFallback {
        @Test
        @DisplayName("성공 - 평가 조회")
        void getFallbackDraftEvaluation_success() {
            ResponseEvaluation response = new ResponseEvaluation(4.5f, List.of("장점: 전체적으로 잘 작성되었습니다.", "개선: 설치 방법을 추가하면 좋겠습니다."));

            given(redisPortOut.getObject(anyString(), any(TypeReference.class)))
                    .willReturn(response);

            ResponseEvaluation result = repositoryService.fallbackDraftEvaluation("1");

            assertThat(result).isNotNull().isEqualTo(response);

            verify(redisPortOut, times(1)).getObject(anyString(), any());
            verify(ssePortOut, times(1)).deleteEmitter(anyString());
            verify(redisPortOut, times(1)).delete(anyString());
        }

        @Test
        @DisplayName("실패 - 평가 없음")
        void getFallbackDraftEvaluation_notFound() {
            given(redisPortOut.getObject(anyString(), any(TypeReference.class)))
                    .willReturn(null);

            assertThatThrownBy(() -> repositoryService.fallbackDraftEvaluation("1"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.FALLBACK_NOT_FOUND);

            verify(redisPortOut, times(1)).getObject(anyString(), any());
            verify(ssePortOut, never()).deleteEmitter(anyString());
            verify(redisPortOut, never()).delete(anyString());
        }

        @Test
        @DisplayName("성공 - README 생성")
        void createReadme_success() {
            ResponseSections response = new ResponseSections(
                    List.of(fixtureMonkey.giveMeOne(ResponseSections.Section.class))
            );

            given(redisPortOut.getObject(anyString(), any(TypeReference.class)))
                    .willReturn(response);

            ResponseSections result = repositoryService.fallbackGenerateReadme("1");

            assertThat(result).isNotNull().isEqualTo(response);

            verify(redisPortOut, times(1)).getObject(anyString(), any());
            verify(ssePortOut, times(1)).deleteEmitter(anyString());
            verify(redisPortOut, times(1)).delete(anyString());
        }

        @Test
        @DisplayName("실패 - 생성된 README 없음")
        void createReadme_notFound() {
            given(redisPortOut.getObject(anyString(), any(TypeReference.class)))
                    .willReturn(null);

            assertThatThrownBy(() -> repositoryService.fallbackGenerateReadme("1"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.FALLBACK_NOT_FOUND);

            verify(redisPortOut, times(1)).getObject(anyString(), any());
            verify(ssePortOut, never()).deleteEmitter(anyString());
            verify(redisPortOut, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("createPullRequest - PR 생성")
    class CreatePullRequest {
        @Test
        @DisplayName("성공")
        void createPullRequest_success() {
            RequestPull request = new RequestPull("main", "new content");

            given(repositoryPortOut.getRecentSHA(any())).willReturn("recent-sha");
            given(repositoryPortOut.getReadmeSHA(any())).willReturn("readme-sha");
            given(repositoryPortOut.createPullRequest(any())).willReturn("https://github.com/pr/1");

            ResponsePull response = repositoryService.createPullRequest(request, USER_ID, OWNER, NAME);

            assertThat(response.htmlUrl()).isEqualTo("https://github.com/pr/1");
            verify(repositoryPortOut, times(1)).createBranch(any());
            verify(repositoryPortOut, times(1)).push(any());
        }

        @Test
        @DisplayName("실패 - README 파일 Push")
        void createPullRequest_failure_during_push() {
            RequestPull request = new RequestPull("main", "new content");

            given(repositoryPortOut.getRecentSHA(any())).willReturn("recent-sha");
            given(repositoryPortOut.getReadmeSHA(any())).willReturn("readme-sha");

            // push 단계에서 에러 발생
            doThrow(new RuntimeException("GitHub API Push Error"))
                    .when(repositoryPortOut).push(any(ReadmePushCommand.class));

            assertThatThrownBy(() -> repositoryService.createPullRequest(request, USER_ID, OWNER, NAME))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.PUSH_FAILED);

            verify(repositoryPortOut, never()).createPullRequest(any());
            verify(repositoryPortOut).deleteBranch(any(RepoBranchCommand.class));
        }

        @Test
        @DisplayName("실패 - Pull Request")
        void createPullRequest_failure_during_pr_creation() {
            RequestPull request = new RequestPull("main", "new content");

            given(repositoryPortOut.getRecentSHA(any())).willReturn("recent-sha");
            given(repositoryPortOut.getReadmeSHA(any())).willReturn("readme-sha");

            doThrow(new RuntimeException("GitHub API PR Creation Error"))
                    .when(repositoryPortOut).createPullRequest(any(CreatePullRequestCommand.class));

            assertThatThrownBy(() -> repositoryService.createPullRequest(request, USER_ID, OWNER, NAME))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.PR_CREATION_FAILED);

            verify(repositoryPortOut).deleteBranch(any(RepoBranchCommand.class));
        }
    }

    @Nested
    @DisplayName("evaluateDraftReadme - 초안 평가 (비동기)")
    class EvaluateDraftReadme {
        private final String taskId = "eval-task-123";
        private final RequestDraftEvaluation request = new RequestDraftEvaluation("main", "my readme content");
        private final EvaluationContentResult gptResponse = new EvaluationContentResult(90, List.of("Great job!"));

        @BeforeEach
        void setUpGPTMock() {
            lenient().when(gptPortOut.evaluateReadme(any())).thenReturn(gptResponse);
        }

        @Test
        @DisplayName("성공 - Cache Hit")
        void evaluateDraftReadme_success_cache_hit() {
            String sha = "latest-sha-123";
            when(repositoryPortOut.getRecentSHA(any())).thenReturn(sha);

            // Redis에서 데이터를 즉시 반환하도록 모킹 (Cache Hit)
            when(redisPortOut.get(contains("readme"))).thenReturn("cached readme");
            when(redisPortOut.getObject(contains("commits"), any())).thenReturn(Collections.emptyList());
            when(redisPortOut.getObject(contains("languages"), any())).thenReturn(Collections.emptyList());
            when(redisPortOut.getObject(contains("tree"), any())).thenReturn(Collections.emptyList());

            GPTRepositoryInfoResult repoInfo = new GPTRepositoryInfoResult(new String[]{"Java"}, "small", new String[]{"src/Main.java"}, new String[]{});
            when(redisPortOut.getObject(contains("tech-stack"), any())).thenReturn(repoInfo);

            when(redisPortOut.get(contains("entry"))).thenReturn("encrypted-entry");
            when(objectCipherPortOut.decrypt(eq("encrypted-entry"), any())).thenReturn(Collections.emptyList());

            when(redisPortOut.get(contains("importance"))).thenReturn("encrypted-importance");
            when(objectCipherPortOut.decrypt(eq("encrypted-importance"), any())).thenReturn(Collections.emptyList());

            repositoryService.evaluateDraftReadme(request, taskId, USER_ID, OWNER, NAME);

            // 외부 API 직접 호출이 되지 않았는지 검증 (Cache Hit)
            verify(repositoryPortOut, never()).getReadmeContent(any());
            verify(repositoryPortOut, never()).getContributors(any());
            verify(repositoryPortOut, never()).getRepositoryLanguages(any());
            verify(gptPortOut, never()).getRepositoryInfo(anyString(), any());

            verify(ssePortOut).sendCompletion(eq(taskId), eq(SSETaskName.COMPLETION_EVALUATE_DRAFT.getTaskName()), any(ResponseEvaluation.class));
        }

        @Test
        @DisplayName("실패 - 커밋 내역 없음")
        void evaluateDraftReadme_success_cache_miss() {
            when(repositoryPortOut.getRecentSHA(any())).thenReturn(null);

            repositoryService.evaluateDraftReadme(request, taskId, USER_ID, OWNER, NAME);

            Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                verify(ssePortOut).sendCompletion(eq(taskId), eq(SSETaskName.COMPLETION_EVALUATE_DRAFT_ERROR.getTaskName()), any(ResponseEntity.class));
            });
        }
    }

    @Nested
    @DisplayName("generateDraftReadme - README 생성 (비동기)")
    class GenerateDraftReadme {
        private final String taskId = "gen-task-456";
        private final RequestGeneration request = new RequestGeneration("main");
        private final String generatedReadme = "# Draft Readme\nEnjoy your code!";

        @BeforeEach
        void setUpGPTAndSectionMock() {
            lenient().when(gptPortOut.generateDraftReadme(any())).thenReturn(generatedReadme);

            Project project = mock(Project.class);
            lenient().when(project.getId()).thenReturn(1L);
            lenient().when(projectPortOut.getByUserIdAndRepoFullName(anyLong(), anyString())).thenReturn(Optional.of(project));

            Section section = mock(Section.class);
            lenient().when(sectionPortOut.saveAll(anyList())).thenReturn(List.of(section));
        }

        @Test
        @DisplayName("성공 - Partial Cache Hit")
        void generateDraftReadme_success_partial_cache_hit() {
            String sha = "latest-sha-123";
            when(repositoryPortOut.getRecentSHA(any())).thenReturn(sha);

            // Readme는 캐시에 있음
            when(redisPortOut.get(contains("readme"))).thenReturn("cached readme");

            // Languages는 캐시 만료로 null 반환 (Cache Miss)
            when(redisPortOut.getObject(contains("languages"), any())).thenReturn(null);

            when(repositoryPortOut.getRepositoryLanguages(any())).thenReturn(List.of(new RepositoryLanguageResult("Java", 100L)));

            when(redisPortOut.getObject(contains("commits"), any())).thenReturn(Collections.emptyList());
            when(redisPortOut.getObject(contains("tree"), any())).thenReturn(Collections.emptyList());
            when(redisPortOut.getObject(contains("tech-stack"), any())).thenReturn(
                    new GPTRepositoryInfoResult(new String[]{"Java"}, "small", new String[]{}, new String[]{})
            );
            when(redisPortOut.get(contains("entry"))).thenReturn("enc");
            when(objectCipherPortOut.decrypt(eq("enc"), any())).thenReturn(Collections.emptyList());
            when(redisPortOut.get(contains("importance"))).thenReturn("enc");

            when(sectionPortOut.getSectionsByUserIdAndRepoFullName(anyLong(), anyString())).thenReturn(List.of(mock(Section.class)));

            repositoryService.generateDraftReadme(request, taskId, USER_ID, OWNER, NAME);

            verify(repositoryPortOut, never()).getReadmeContent(any()); // 캐시 탔으므로 호출 X
            verify(repositoryPortOut, times(1)).getRepositoryLanguages(any()); // 캐시 만료됐으므로 호출 O

            // 새로 조회해온 Language 정보를 Redis에 갱신(setObject)했는지 검증
            verify(redisPortOut).setObject(contains("languages"), anyList(), any());
            verify(sectionPortOut).deleteAllByUserIdAndRepoFullName(USER_ID, OWNER + "/" + NAME);
        }

        @Test
        @DisplayName("실패 - 성공 응답 실패 (폴백 저장)")
        void generateDraftReadme_failure_sse_send_caches_result() {
            String sha = "latest-sha-123";
            when(repositoryPortOut.getRecentSHA(any())).thenReturn(sha);

            // 모든 캐시가 존재한다고 가정
            lenient().when(redisPortOut.get(anyString())).thenReturn("cache");
            lenient().when(objectCipherPortOut.decrypt(anyString(), any())).thenReturn(Collections.emptyList());

            GPTRepositoryInfoResult repoInfo = new GPTRepositoryInfoResult(new String[]{"Java"}, "small", new String[]{}, new String[]{});

            // 요청하는 키에 맞춰서 타입을 반환해 NPE 방지
            lenient().when(redisPortOut.getObject(anyString(), any())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                if (key.contains("tech-stack")) {
                    return repoInfo;
                }

                return Collections.emptyList();
            });

            // SSE 전송이 실패하도록 모킹
            when(ssePortOut.sendCompletion(anyString(), anyString(), any())).thenReturn(false);

            repositoryService.generateDraftReadme(request, taskId, USER_ID, OWNER, NAME);

            verify(redisPortOut).setObjectIfAbsent(contains(RedisKey.SSE_EMITTER_GENERATION_KEY.getValue()), any(ResponseSections.class), any());
        }

        @Test
        @DisplayName("실패 - 커밋 내역 없음")
        void generateDraftReadme_success_cache_miss() {
            when(repositoryPortOut.getRecentSHA(any())).thenReturn(null);

            repositoryService.generateDraftReadme(request, taskId, USER_ID, OWNER, NAME);

            Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                verify(ssePortOut).sendCompletion(eq(taskId), eq(SSETaskName.COMPLETION_GENERATE_ERROR.getTaskName()), any(ResponseEntity.class));
            });
        }
    }
}