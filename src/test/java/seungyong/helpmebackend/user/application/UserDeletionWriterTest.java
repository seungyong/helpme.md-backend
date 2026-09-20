package seungyong.helpmebackend.user.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.entity.UserDeletion;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@ExtendWith(MockitoExtension.class)
class UserDeletionWriterTest {
    @Mock private UserPortOut userPortOut;
    @Mock private UserDeletionPortOut userDeletionPortOut;
    @Mock private ProjectDeletionPortOut projectDeletionPortOut;

    @Test
    @DisplayName("첫 탈퇴 요청은 사용자와 모든 프로젝트를 같은 requestedAt으로 차단")
    void requestsUserAndProjects() {
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        User active = user(1L);
        User deleting = deletingUser(requestedAt);

        given(userDeletionPortOut.requestDeletion(1L, requestedAt)).willReturn(deleting);

        User result = new UserDeletionWriter(
                userDeletionPortOut, projectDeletionPortOut
        ).request(1L, requestedAt);

        assertThat(result.getDeletion().requestedAt()).isEqualTo(requestedAt);
        verify(projectDeletionPortOut).requestAllByUserId(1L, requestedAt);
    }

    @Test
    @DisplayName("반복 DELETE는 최초 requestedAt을 반환하고 새 정리 작업을 만들지 않음")
    void repeatedRequestIsIdempotent() {
        OffsetDateTime firstRequestedAt = OffsetDateTime.parse("2026-09-14T09:00:00Z");
        User deleting = deletingUser(firstRequestedAt);
        given(userDeletionPortOut.requestDeletion(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any())).willReturn(deleting);

        User result = new UserDeletionWriter(
                userDeletionPortOut, projectDeletionPortOut
        ).request(1L, OffsetDateTime.parse("2026-09-14T10:00:00Z"));

        assertThat(result.getDeletion().requestedAt()).isEqualTo(firstRequestedAt);
        verify(projectDeletionPortOut).requestAllByUserId(1L, firstRequestedAt);
    }

    private User deletingUser(OffsetDateTime requestedAt) {
        return User.builder()
                .id(1L)
                .githubUser(user(1L).getGithubUser())
                .status(UserStatus.DELETING)
                .deletion(new UserDeletion(requestedAt, null, null))
                .build();
    }
}
