package seungyong.helpmebackend.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

class UserDeletionLifecycleTest {
    @Test
    @DisplayName("반복 탈퇴 요청은 최초 requestedAt을 보존하고 이전 오류를 지움")
    void preservesFirstRequestTime() {
        OffsetDateTime first = OffsetDateTime.parse("2026-09-14T09:00:00Z");
        User target = User.builder()
                .id(1L)
                .githubUser(user(1L).getGithubUser())
                .status(UserStatus.DELETE_FAILED)
                .deletion(new UserDeletion(first, "SERVER_500", "failed"))
                .build();

        target.requestDeletion(OffsetDateTime.parse("2026-09-14T10:00:00Z"));

        assertThat(target.getStatus()).isEqualTo(UserStatus.DELETING);
        assertThat(target.getDeletion().requestedAt()).isEqualTo(first);
        assertThat(target.getDeletion().errorCode()).isNull();
    }
}
