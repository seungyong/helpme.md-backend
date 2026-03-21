package seungyong.helpmebackend.user.domain.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;

import static org.assertj.core.api.Assertions.*;

public class UserTest {
    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Test
    @DisplayName("토큰 비교 - 같은 토큰")
    void isDiffToken_returnFalse() {
        String token = "same-token";

        User user = fixtureMonkey.giveMeBuilder(User.class)
                .set("githubUser.githubToken.value", token)
                .sample();

        assert user != null;
        boolean result = user.isDiffToken(token);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰 비교 - 다른 토큰")
    void isDiffToken_returnTrue() {
        String currentToken = "old-token";
        String newToken = "new-token";

        User user = fixtureMonkey.giveMeBuilder(User.class)
                .set("githubUser.githubToken.value", currentToken)
                .sample();

        assert user != null;
        boolean result = user.isDiffToken(newToken);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("토큰 변경 - 성공")
    void updateToken_success() {
        String currentToken = "old-token";
        String newToken = "new-token";

        User user = fixtureMonkey.giveMeBuilder(User.class)
                .set("githubUser.githubToken.value", currentToken)
                .sample();

        assert user != null;
        user.updateGithubToken(new EncryptedToken(newToken));

        assertThat(user.getGithubUser().getGithubToken().value()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("토큰 변경 - 실패 (null)")
    void updateToken_failure() {
        String currentToken = "old-token";

        User user = fixtureMonkey.giveMeBuilder(User.class)
                .set("githubUser.githubToken.value", currentToken)
                .sample();

        assert user != null;
        assertThatThrownBy(() -> user.updateGithubToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새로운 토큰은 null일 수 없습니다.");
    }
}
