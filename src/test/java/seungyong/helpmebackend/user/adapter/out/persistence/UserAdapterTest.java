package seungyong.helpmebackend.user.adapter.out.persistence;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@JpaTest
public class UserAdapterTest {
    @Autowired private UserPortOut userPortOut;
    @Autowired private UserJpaRepository userJpaRepository;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Test
    @DisplayName("유저 저장 - 성공")
    void save_user_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        assert user != null;

        User savedUser = userPortOut.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getGithubUser().getGithubId())
                .isEqualTo(user.getGithubUser().getGithubId());

        assertThat(userJpaRepository.findById(savedUser.getId())).isPresent();
    }

    @Test
    @DisplayName("유저 삭제 - 성공")
    void delete_user_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        assert user != null;

        User savedUser = userPortOut.save(user);
        Long userId = savedUser.getId();

        userPortOut.delete(savedUser);

        assertThat(userJpaRepository.findById(userId)).isNotPresent();
    }

    @Test
    @DisplayName("유저 ID 조회 - 성공")
    void get_by_id_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        assert user != null;

        User savedUser = userPortOut.save(user);
        Long userId = savedUser.getId();

        User foundUser = userPortOut.getById(userId);

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("유저 ID 조회 - 실패 (존재하지 않는 ID)")
    void get_by_id_failure_not_found() {
        Long nonExistentId = 999L;

        assertThatThrownBy(() -> userPortOut.getById(nonExistentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("깃허브 ID로 유저 조회 - 성공")
    void get_by_github_id_success() {
         User user = fixtureMonkey.giveMeBuilder(User.class)
                 .setNull("id")
                 .set("githubUser.githubToken.value", "test-token")
                 .sample();

         assert user != null;

         User savedUser = userPortOut.save(user);
         Long githubId = savedUser.getGithubUser().getGithubId();

         User foundUser = userPortOut.getByGithubId(githubId).orElse(null);

         assertThat(foundUser).isNotNull();
         assertThat(foundUser.getGithubUser().getGithubId()).isEqualTo(githubId);
    }

    @Test
    @DisplayName("깃허브 ID로 유저 조회 - 실패 (존재하지 않는 Github ID)")
    void get_by_github_id_failure_not_found() {
        Long nonExistentGithubId = 999L;

        assertThat(userPortOut.getByGithubId(nonExistentGithubId)).isEmpty();
    }
}
