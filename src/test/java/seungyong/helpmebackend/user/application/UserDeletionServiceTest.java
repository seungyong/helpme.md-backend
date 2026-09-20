package seungyong.helpmebackend.user.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.notion.application.port.in.NotionDeletionPortIn;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.user.application.port.in.command.DeleteUserCommand;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.entity.UserDeletion;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@ExtendWith(MockitoExtension.class)
class UserDeletionServiceTest {
    @Mock private UserDeletionWriter deletionWriter;
    @Mock private UserDeletionPortOut userDeletionPortOut;
    @Mock private ProjectDeletionPortOut projectDeletionPortOut;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private NotionDeletionPortIn notionDeletionPortIn;
    @Mock private RedisPortOut redisPortOut;

    private UserDeletionService service;

    @BeforeEach
    void setUp() {
        service = new UserDeletionService(
                deletionWriter,
                userDeletionPortOut,
                new UserDeletionProcessor(userDeletionPortOut, projectDeletionPortOut,
                        projectPortOut, notionDeletionPortIn),
                redisPortOut
        );
        ReflectionTestUtils.setField(service, "gracePeriodSeconds", 300L);
        ReflectionTestUtils.setField(service, "retryDelaySeconds", 60L);
    }

    @Test
    @DisplayName("탈퇴 요청은 즉시 deleting과 sign_out을 반환하고 현재 refresh token을 정리")
    void requestsDeletion() {
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        given(deletionWriter.request(org.mockito.ArgumentMatchers.eq(1L), any()))
                .willReturn(deletingUser(requestedAt));

        var result = service.requestDeletion(new DeleteUserCommand(1L, true, "refresh-token"));

        assertThat(result.status()).isEqualTo("deleting");
        assertThat(result.requestedAt()).isEqualTo(requestedAt);
        assertThat(result.requiredAction()).isEqualTo("sign_out");
        verify(redisPortOut).delete("refresh-token:refresh-token");
    }

    @Test
    @DisplayName("남은 프로젝트가 있으면 프로젝트 삭제 상태만 보정하고 user hard delete를 보류")
    void waitsForProjectCleanup() {
        given(userDeletionPortOut.lockClaim(any(), any())).willReturn(true);
        User deleting = deletingUser(OffsetDateTime.parse("2026-09-14T09:00:00Z"));
        given(userDeletionPortOut.claimNext(any(), any(), any()))
                .willReturn(Optional.of(deleting));
        given(projectPortOut.countByUserId(1L)).willReturn(1L);

        service.processNext();

        verify(projectDeletionPortOut).requestAllByUserId(
                1L, deleting.getDeletion().requestedAt()
        );
        verify(notionDeletionPortIn, never()).deleteUserConnection(any());
        verify(userDeletionPortOut, never()).hardDelete(any());
    }

    @Test
    @DisplayName("프로젝트와 Notion 연결 정리가 끝난 뒤 user를 hard delete")
    void hardDeletesAfterCleanup() {
        given(userDeletionPortOut.lockClaim(any(), any())).willReturn(true);
        User deleting = deletingUser(OffsetDateTime.parse("2026-09-14T09:00:00Z"));
        given(userDeletionPortOut.claimNext(any(), any(), any()))
                .willReturn(Optional.of(deleting));
        given(projectPortOut.countByUserId(1L)).willReturn(0L);

        service.processNext();

        var order = org.mockito.Mockito.inOrder(notionDeletionPortIn, userDeletionPortOut);
        order.verify(notionDeletionPortIn).deleteUserConnection(1L);
        order.verify(userDeletionPortOut).hardDelete(1L);
        verify(userDeletionPortOut, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Notion 정리가 실패하면 user를 남기고 자동 재시도 오류를 기록")
    void recordsCleanupFailure() {
        given(userDeletionPortOut.lockClaim(any(), any())).willReturn(true);
        User deleting = deletingUser(OffsetDateTime.parse("2026-09-14T09:00:00Z"));
        given(userDeletionPortOut.claimNext(any(), any(), any()))
                .willReturn(Optional.of(deleting));
        given(projectPortOut.countByUserId(1L)).willReturn(0L);
        org.mockito.Mockito.doThrow(new IllegalStateException("notion unavailable"))
                .when(notionDeletionPortIn).deleteUserConnection(1L);

        service.processNext();

        verify(userDeletionPortOut).markFailed(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(deleting.getUpdatedAt()),
                org.mockito.ArgumentMatchers.eq("SERVER_500"),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(userDeletionPortOut, never()).hardDelete(any());
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
