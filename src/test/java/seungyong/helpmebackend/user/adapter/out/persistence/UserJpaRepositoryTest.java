package seungyong.helpmebackend.user.adapter.out.persistence;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.BuilderArbitraryIntrospector;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@JpaTest
public class UserJpaRepositoryTest {
    @Autowired private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("Github ID로 유저 조회 - 성공")
    void findByGithubId_Success() {
        UserJpaEntity user = FixtureMonkey.builder()
                .objectIntrospector(BuilderArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(UserJpaEntity.class)
                .set("id", null)
                .set("githubToken", new EncryptedToken("encrypted-token"))
                .sample();

        assert user != null;
        userJpaRepository.save(user);

        Long githubId = user.getGithubId();

        assertThat(userJpaRepository.findByGithubId(githubId))
                .isPresent()
                .get()
                .satisfies(foundUser -> {
                    Assertions.assertThat(foundUser.getId()).isNotNull();
                    Assertions.assertThat(foundUser.getGithubId()).isEqualTo(githubId);
                });
    }
}
