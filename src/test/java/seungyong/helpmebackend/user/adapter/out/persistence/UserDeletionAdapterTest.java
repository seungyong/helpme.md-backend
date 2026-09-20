package seungyong.helpmebackend.user.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class UserDeletionAdapterTest {
    @Autowired private UserPortOut userPortOut;
    @Autowired private UserDeletionPortOut deletionPortOut;

    @Test
    @DisplayName("탈퇴 요청을 한 worker만 lease로 claim하고 hard delete")
    void claimsOnceAndHardDeletes() {
        User saved = userPortOut.save(user(null, "deletion-user-token"));
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T09:00:00Z");
        User deleting = deletionPortOut.requestDeletion(saved.getId(), requestedAt);
        assertThat(deleting.getStatus()).isEqualTo(UserStatus.DELETING);

        OffsetDateTime claimedAt = OffsetDateTime.now().plusMinutes(10);
        assertThat(deletionPortOut.claimNext(
                claimedAt, claimedAt.minusMinutes(5), claimedAt.minusMinutes(1)
        )).isPresent();
        assertThat(deletionPortOut.claimNext(
                claimedAt, claimedAt.minusMinutes(5), claimedAt.minusMinutes(1)
        )).isEmpty();

        deletionPortOut.hardDelete(saved.getId());
        assertThatThrownByUserLookup(saved.getId());
    }

    private void assertThatThrownByUserLookup(Long userId) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userPortOut.getById(userId))
                .isInstanceOf(seungyong.helpmebackend.global.exception.CustomException.class);
    }
}
