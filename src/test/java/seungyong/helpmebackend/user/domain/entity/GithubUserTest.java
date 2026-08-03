package seungyong.helpmebackend.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.githubUser;

public class GithubUserTest {
    @Test
    @DisplayName("토큰 수정 - 성공")
    void updateGithubToken_success() {
        String currentToken = "old-token";
        String newToken = "new-token";

        GithubUser user = githubUser(1001L, currentToken);

        user.updateGithubToken(new EncryptedToken(newToken));

        assertThat(user.getGithubToken().value()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("토큰 수정 - 실패 (null)")
    void updateGithubToken_failNull() {
        String currentToken = "old-token";

        GithubUser user = githubUser(1001L, currentToken);

        assertThatThrownBy(() -> user.updateGithubToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새로운 토큰은 null일 수 없습니다.");
    }

    @Test
    @DisplayName("GitHub 토큰 검증 결과와 시각을 함께 기록")
    void recordTokenVerification_success() {
        GithubUser user = githubUser();
        OffsetDateTime verifiedAt = OffsetDateTime.of(
                2026, 8, 1, 12, 0, 0, 0, ZoneOffset.UTC
        );

        user.recordTokenVerification(GithubTokenStatus.REVOKED, verifiedAt);

        assertThat(user.getTokenStatus()).isEqualTo(GithubTokenStatus.REVOKED);
        assertThat(user.getTokenVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    @DisplayName("unknown은 GitHub 토큰 검증 결과로 기록할 수 없음")
    void recordTokenVerification_fail_unknown() {
        GithubUser user = githubUser();

        assertThatThrownBy(() -> user.recordTokenVerification(
                GithubTokenStatus.UNKNOWN,
                OffsetDateTime.now(ZoneOffset.UTC)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
