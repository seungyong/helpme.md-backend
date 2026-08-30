package seungyong.helpmebackend.section.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.github.application.port.in.GithubRepositoryAccessPortIn;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadmeComponentRepositoryAccessResolverTest {
    private static final Long USER_ID = 1L;
    private static final String OWNER = "octocat";
    private static final String NAME = "helpme-md";
    private static final String FULL_NAME = OWNER + "/" + NAME;

    @Mock private ProjectPortOut projectPortOut;
    @Mock private GithubRepositoryAccessPortIn githubRepositoryAccessPortIn;
    @Mock private RedisPortOut redisPortOut;

    private ReadmeComponentRepositoryAccessResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ReadmeComponentRepositoryAccessResolver(
                projectPortOut, githubRepositoryAccessPortIn, redisPortOut
        );
    }

    @Test
    @DisplayName("권한 캐시가 있으면 GitHub API를 호출하지 않는다")
    void resolveWritable_cacheHit() {
        Project project = project();
        given(projectPortOut.getByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                .willReturn(Optional.of(project));
        given(redisPortOut.exists(anyString())).willReturn(true);

        Project resolved = resolver.resolveWritable(USER_ID, OWNER, NAME);

        assertThat(resolved).isSameAs(project);
        verify(githubRepositoryAccessPortIn, never())
                .getRepository(any(), any(), any());
    }

    @Test
    @DisplayName("권한 캐시가 없을 때만 GitHub App Repository 쓰기 권한을 1회 확인한다")
    void resolveWritable_cacheMiss() {
        Project project = project();
        given(projectPortOut.getByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                .willReturn(Optional.of(project));
        given(redisPortOut.exists(anyString())).willReturn(false);
        given(githubRepositoryAccessPortIn.getRepository(USER_ID, 20L, 30L))
                .willReturn(repository(true));

        Project resolved = resolver.resolveWritable(USER_ID, OWNER, NAME);

        assertThat(resolved).isSameAs(project);
        verify(githubRepositoryAccessPortIn).getRepository(USER_ID, 20L, 30L);
        verify(redisPortOut).set(anyString(), anyString(), any(Instant.class));
    }

    @Test
    @DisplayName("Repository 쓰기 권한이 없으면 GITHUB_40302")
    void resolveWritable_permissionDenied() {
        given(projectPortOut.getByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                .willReturn(Optional.of(project()));
        given(redisPortOut.exists(anyString())).willReturn(false);
        given(githubRepositoryAccessPortIn.getRepository(USER_ID, 20L, 30L))
                .willReturn(repository(false));

        assertThatThrownBy(() -> resolver.resolveWritable(USER_ID, OWNER, NAME))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(GithubErrorCode.GITHUB_PERMISSION_DENIED));
        verify(redisPortOut, never()).set(anyString(), anyString(), any(Instant.class));
    }

    @Test
    @DisplayName("Redis 조회 실패 시 API를 막지 않고 GitHub 권한을 직접 확인한다")
    void resolveWritable_redisFailureFallsBack() {
        given(projectPortOut.getByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                .willReturn(Optional.of(project()));
        given(redisPortOut.exists(anyString()))
                .willThrow(new CustomException(GlobalErrorCode.REDIS_ERROR));
        given(githubRepositoryAccessPortIn.getRepository(USER_ID, 20L, 30L))
                .willReturn(repository(true));

        Project resolved = resolver.resolveWritable(USER_ID, OWNER, NAME);

        assertThat(resolved.getRepoFullName()).isEqualTo(FULL_NAME);
        verify(githubRepositoryAccessPortIn).getRepository(USER_ID, 20L, 30L);
    }

    private Project project() {
        return Project.builder()
                .id(10L)
                .userId(USER_ID)
                .repoFullName(FULL_NAME)
                .githubInstallationId(20L)
                .githubRepoId(30L)
                .defaultBranch("main")
                .build();
    }

    private GithubRepository repository(boolean push) {
        return new GithubRepository(
                30L,
                FULL_NAME,
                false,
                "main",
                new GithubRepository.Permissions(false, push)
        );
    }
}
