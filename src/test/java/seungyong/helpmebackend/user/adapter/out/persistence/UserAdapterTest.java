package seungyong.helpmebackend.user.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.entity.UserDeletion;
import seungyong.helpmebackend.user.domain.entity.UserPlan;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;
import seungyong.helpmebackend.user.domain.type.PlanCode;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

@JpaTest
public class UserAdapterTest {
    @Autowired private UserPortOut userPortOut;
    @Autowired private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("유저 저장 - 성공")
    void save_user_success() {
        User user = createUser();

        User savedUser = userPortOut.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getGithubUser().getGithubId())
                .isEqualTo(user.getGithubUser().getGithubId());

        assertThat(userJpaRepository.findById(savedUser.getId())).isPresent();
    }

    @Test
    @DisplayName("유저 삭제 - 성공")
    void delete_user_success() {
        User user = createUser();

        User savedUser = userPortOut.save(user);
        Long userId = savedUser.getId();

        userPortOut.delete(savedUser);

        assertThat(userJpaRepository.findById(userId)).isNotPresent();
    }

    @Test
    @DisplayName("유저 ID 조회 - 성공")
    void get_by_id_success() {
        User user = createUser();

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
         User user = createUser();

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

    @Test
    @DisplayName("유저 저장·조회 시 플랜, GitHub 검증, 삭제 상태를 보존한다")
    void saveAndLoad_preservesExtendedUserState() {
        OffsetDateTime now = OffsetDateTime.of(
                2026, 7, 30, 12, 0, 0, 0, ZoneOffset.UTC
        );
        User user = User.builder()
                .githubUser(GithubUser.builder()
                        .name("octocat")
                        .githubId(12345L)
                        .githubToken(new EncryptedToken("encrypted-token"))
                        .tokenStatus(GithubTokenStatus.VALID)
                        .tokenVerifiedAt(now)
                        .build())
                .plan(new UserPlan(PlanCode.PRO, (short) 3, now.plusYears(1)))
                .status(UserStatus.DELETE_FAILED)
                .lastLoginAt(now)
                .deletion(new UserDeletion(now.plusDays(1), "STORAGE_ERROR", "cleanup failed"))
                .build();

        User savedUser = userPortOut.save(user);
        User foundUser = userPortOut.getById(savedUser.getId());

        assertThat(foundUser.getPlan()).isEqualTo(user.getPlan());
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.DELETE_FAILED);
        assertThat(foundUser.getGithubUser().getTokenStatus()).isEqualTo(GithubTokenStatus.VALID);
        assertThat(foundUser.getGithubUser().getTokenVerifiedAt()).isEqualTo(now);
        assertThat(foundUser.getLastLoginAt()).isEqualTo(now);
        assertThat(foundUser.getDeletion()).isEqualTo(user.getDeletion());
    }

    private User createUser() {
        return new User(
                null,
                new GithubUser(
                        "octocat",
                        12345L,
                        new EncryptedToken("encrypted-token")
                )
        );
    }
}
