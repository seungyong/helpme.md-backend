package seungyong.helpmebackend.github.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.github.application.port.out.GithubAppPortOut;
import seungyong.helpmebackend.github.domain.entity.GithubInstallation;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.entity.GithubRepositoryPage;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.github.domain.type.GithubAccountType;
import seungyong.helpmebackend.github.domain.type.GithubRepositorySelection;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@ExtendWith(MockitoExtension.class)
class GithubAppServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long INSTALLATION_ID = 9001L;
    private static final String RAW_TOKEN = "raw-github-token";

    @Mock private GithubAppPortOut githubAppPortOut;
    @Mock private UserPortOut userPortOut;
    @Mock private CipherPortOut cipherPortOut;
    @Mock private ProjectPortOut projectPortOut;
    @InjectMocks private GithubAppService githubAppService;

    @Nested
    @DisplayName("GitHub 설치 계정 조회")
    class Installations {
        @Test
        @DisplayName("설치가 있으면 appInstalled와 계정 정보를 반환하고 토큰을 검증 처리")
        void success() {
            User user = prepareUser();
            GithubInstallation installation = new GithubInstallation(
                    INSTALLATION_ID,
                    "seungyong",
                    GithubAccountType.USER,
                    GithubRepositorySelection.SELECTED,
                    8
            );
            given(githubAppPortOut.getInstallations(USER_ID, RAW_TOKEN))
                    .willReturn(List.of(installation));

            var result = githubAppService.getInstallations(USER_ID);

            assertThat(result.appInstalled()).isTrue();
            assertThat(result.accounts()).containsExactly(installation);
            assertThat(user.getGithubUser().getTokenStatus()).isEqualTo(GithubTokenStatus.VALID);
            assertThat(user.getGithubUser().getTokenVerifiedAt()).isNotNull();
            verify(userPortOut).save(user);
        }

        @Test
        @DisplayName("설치가 없으면 정상적인 미설치 상태를 반환")
        void success_notInstalled() {
            prepareUser();
            given(githubAppPortOut.getInstallations(USER_ID, RAW_TOKEN)).willReturn(List.of());

            var result = githubAppService.getInstallations(USER_ID);

            assertThat(result.appInstalled()).isFalse();
            assertThat(result.accounts()).isEmpty();
        }

        @Test
        @DisplayName("GitHub 연결 회수 시 token status를 revoked로 저장")
        void failure_connectionRevoked() {
            User user = prepareUser();
            given(githubAppPortOut.getInstallations(USER_ID, RAW_TOKEN))
                    .willThrow(new CustomException(GithubErrorCode.GITHUB_CONNECTION_REVOKED));

            assertThatThrownBy(() -> githubAppService.getInstallations(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            GithubErrorCode.GITHUB_CONNECTION_REVOKED
                    );

            assertThat(user.getGithubUser().getTokenStatus()).isEqualTo(GithubTokenStatus.REVOKED);
            verify(userPortOut).save(user);
        }
    }

    @Nested
    @DisplayName("GitHub 설치 Repository 조회")
    class Repositories {
        @Test
        @DisplayName("GitHub canonical 정보에 현재 사용자의 프로젝트 중복 여부를 결합")
        void success() {
            prepareUser();
            GithubRepository connected = repository(101L, "seungyong/helpme.md", true);
            GithubRepository available = repository(102L, "seungyong/portfolio", false);
            given(githubAppPortOut.getRepositories(
                    USER_ID, RAW_TOKEN, INSTALLATION_ID, "help", 1, 30
            )).willReturn(new GithubRepositoryPage(
                    List.of(connected, available),
                    "2",
                    true
            ));
            given(projectPortOut.getConnectedGithubRepoIds(USER_ID, List.of(101L, 102L)))
                    .willReturn(Set.of(101L));

            var result = githubAppService.getRepositories(
                    USER_ID, INSTALLATION_ID, " help ", null, null
            );

            assertThat(result.items()).hasSize(2);
            assertThat(result.items().get(0).alreadyConnected()).isTrue();
            assertThat(result.items().get(1).alreadyConnected()).isFalse();
            assertThat(result.page().nextCursor()).isEqualTo("2");
            assertThat(result.page().hasNext()).isTrue();
        }

        @Test
        @DisplayName("숫자 cursor를 GitHub 페이지 번호로 전달")
        void success_cursor() {
            prepareUser();
            given(githubAppPortOut.getRepositories(
                    USER_ID, RAW_TOKEN, INSTALLATION_ID, "", 3, 10
            )).willReturn(new GithubRepositoryPage(List.of(), null, false));

            githubAppService.getRepositories(USER_ID, INSTALLATION_ID, null, "3", 10);

            verify(githubAppPortOut).getRepositories(
                    USER_ID, RAW_TOKEN, INSTALLATION_ID, "", 3, 10
            );
        }

        @Test
        @DisplayName("cursor와 size가 유효하지 않으면 GitHub를 호출하지 않고 400")
        void failure_invalidPage() {
            assertThatThrownBy(() -> githubAppService.getRepositories(
                    USER_ID, INSTALLATION_ID, null, "invalid", 30
            )).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);

            assertThatThrownBy(() -> githubAppService.getRepositories(
                    USER_ID, INSTALLATION_ID, null, "0", 30
            )).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);

            assertThatThrownBy(() -> githubAppService.getRepositories(
                    USER_ID, INSTALLATION_ID, null, null, 101
            )).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);
        }
    }

    private User prepareUser() {
        User user = user(USER_ID);
        given(userPortOut.getById(USER_ID)).willReturn(user);
        given(cipherPortOut.decrypt(user.getGithubUser().getGithubToken().value()))
                .willReturn(RAW_TOKEN);
        return user;
    }

    private GithubRepository repository(Long id, String fullName, boolean privateRepository) {
        return new GithubRepository(
                id,
                fullName,
                privateRepository,
                "main",
                new GithubRepository.Permissions(true, true)
        );
    }
}
