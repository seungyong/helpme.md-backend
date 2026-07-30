package seungyong.helpmebackend.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

public class UserTest {
    @Test
    @DisplayName("토큰 변경 - 성공")
    void updateToken_success() {
        String currentToken = "old-token";
        String newToken = "new-token";

        User user = createActiveUser(currentToken);

        user.updateGithubToken(new EncryptedToken(newToken));

        assertThat(user.getGithubUser().getGithubToken().value()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("토큰 변경 - 실패 (null)")
    void updateToken_failure() {
        String currentToken = "old-token";

        User user = createActiveUser(currentToken);

        assertThatThrownBy(() -> user.updateGithubToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새로운 토큰은 null일 수 없습니다.");
    }

    @Test
    @DisplayName("활성 사용자만 로그인할 수 있다")
    void authenticationAllowed_onlyActiveUser() {
        GithubUser githubUser = new GithubUser(
                "octocat",
                1L,
                new EncryptedToken("encrypted-token")
        );

        assertThat(new User(1L, githubUser, UserStatus.ACTIVE).isAuthenticationAllowed()).isTrue();
        assertThat(new User(1L, githubUser, UserStatus.DELETING).isAuthenticationAllowed()).isFalse();
        assertThat(new User(1L, githubUser, UserStatus.DELETE_FAILED).isAuthenticationAllowed()).isFalse();
    }

    @Test
    @DisplayName("로그인 성공 시 GitHub 인증 정보와 마지막 로그인 시각을 함께 갱신한다")
    void recordSuccessfulLogin_updatesAuthenticationState() {
        OffsetDateTime authenticatedAt = OffsetDateTime.of(
                2026, 7, 30, 12, 0, 0, 0, ZoneOffset.UTC
        );
        User user = new User(
                1L,
                new GithubUser("old-name", 10L, new EncryptedToken("old-token")),
                UserStatus.ACTIVE
        );
        GithubUser authenticatedGithubUser = GithubUser.authenticated(
                "new-name",
                10L,
                new EncryptedToken("new-token"),
                authenticatedAt
        );

        user.recordSuccessfulLogin(authenticatedGithubUser, authenticatedAt);

        assertThat(user.getGithubUser().getName()).isEqualTo("new-name");
        assertThat(user.getGithubUser().getGithubToken().value()).isEqualTo("new-token");
        assertThat(user.getGithubUser().getTokenStatus()).isEqualTo(GithubTokenStatus.VALID);
        assertThat(user.getGithubUser().getTokenVerifiedAt()).isEqualTo(authenticatedAt);
        assertThat(user.getLastLoginAt()).isEqualTo(authenticatedAt);
    }

    private User createActiveUser(String encryptedToken) {
        return new User(
                1L,
                new GithubUser("octocat", 10L, new EncryptedToken(encryptedToken)),
                UserStatus.ACTIVE
        );
    }
}
