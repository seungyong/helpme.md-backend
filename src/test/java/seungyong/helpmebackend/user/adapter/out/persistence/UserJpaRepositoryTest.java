package seungyong.helpmebackend.user.adapter.out.persistence;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.userJpaEntity;

@Slf4j
@JpaTest
public class UserJpaRepositoryTest {
    @Autowired private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("Github ID로 유저 조회 - 성공")
    void findByGithubId_Success() {
        UserJpaEntity user = userJpaEntity(null, 1001L, "encrypted-token");

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
