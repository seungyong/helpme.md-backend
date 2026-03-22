package seungyong.helpmebackend.user.domain.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;

import static org.assertj.core.api.Assertions.*;

public class GithubUserTest {
    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Test
    @DisplayName("토큰 수정 - 성공")
    void updateGithubToken_success() {
        String currentToken = "old-token";
        String newToken = "new-token";

        GithubUser user = fixtureMonkey.giveMeBuilder(GithubUser.class)
                .set("githubToken.value", currentToken)
                .sample();

        assert user != null;
        user.updateGithubToken(new EncryptedToken(newToken));

        assertThat(user.getGithubToken().value()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("토큰 수정 - 실패 (null)")
    void updateGithubToken_failNull() {
        String currentToken = "old-token";

        GithubUser user = fixtureMonkey.giveMeBuilder(GithubUser.class)
                .set("githubToken.value", currentToken)
                .sample();

        assert user != null;
        assertThatThrownBy(() -> user.updateGithubToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새로운 토큰은 null일 수 없습니다.");
    }
}
