package seungyong.helpmebackend.project.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioDeletionPortIn;
import seungyong.helpmebackend.project.application.port.in.command.DeleteProjectCommand;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectDeletion;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectDeletionServiceTest {
    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private ProjectDeletionPortOut deletionPortOut;
    @Mock private PortfolioDeletionPortIn portfolioDeletionPortIn;

    private ProjectDeletionService service;

    @BeforeEach
    void setUp() {
        service = new ProjectDeletionService(
                projectAccessResolver, deletionPortOut, new ProjectDeletionProcessor(deletionPortOut, portfolioDeletionPortIn)
        );
        ReflectionTestUtils.setField(service, "gracePeriodSeconds", 300L);
        ReflectionTestUtils.setField(service, "retryDelaySeconds", 60L);
    }

    @Test
    @DisplayName("소유자와 확인 문자열을 검증한 뒤 프로젝트를 deleting으로 전환")
    void requestsDeletion() {
        Project active = project(ProjectStatus.ACTIVE, null);
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        Project deleting = project(ProjectStatus.DELETING, requestedAt);
        given(projectAccessResolver.resolveOwned(1L, 101L)).willReturn(active);
        given(deletionPortOut.requestDeletion(org.mockito.ArgumentMatchers.eq(101L), any()))
                .willReturn(deleting);

        var result = service.requestDeletion(
                new DeleteProjectCommand(1L, 101L, " seungyong/helpme.md ")
        );

        assertThat(result.projectId()).isEqualTo(101L);
        assertThat(result.status()).isEqualTo("deleting");
        assertThat(result.requestedAt()).isEqualTo(requestedAt);
    }

    @Test
    @DisplayName("Repository 확인 문자열이 다르면 삭제 상태를 만들지 않음")
    void rejectsDifferentRepositoryName() {
        given(projectAccessResolver.resolveOwned(1L, 101L))
                .willReturn(project(ProjectStatus.ACTIVE, null));

        assertThatThrownBy(() -> service.requestDeletion(
                new DeleteProjectCommand(1L, 101L, "other/repository")
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);

        verify(deletionPortOut, never()).requestDeletion(any(), any());
    }

    @Test
    @DisplayName("Storage 자산 정리 후에만 프로젝트를 hard delete")
    void cleansAssetsBeforeHardDelete() {
        given(deletionPortOut.lockClaim(any(), any())).willReturn(true);
        Project deleting = project(
                ProjectStatus.DELETING,
                OffsetDateTime.parse("2026-09-14T09:00:00Z")
        );
        given(deletionPortOut.claimNext(any(), any(), any()))
                .willReturn(Optional.of(deleting));

        service.processNext();

        var order = org.mockito.Mockito.inOrder(portfolioDeletionPortIn, deletionPortOut);
        order.verify(portfolioDeletionPortIn).deleteProjectAssets(101L);
        order.verify(deletionPortOut).hardDelete(101L);
        verify(deletionPortOut, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    @DisplayName("외부 자산 정리가 실패하면 hard delete하지 않고 자동 재시도 상태를 기록")
    void recordsCleanupFailure() {
        given(deletionPortOut.lockClaim(any(), any())).willReturn(true);
        Project deleting = project(
                ProjectStatus.DELETING,
                OffsetDateTime.parse("2026-09-14T09:00:00Z")
        );
        given(deletionPortOut.claimNext(any(), any(), any()))
                .willReturn(Optional.of(deleting));
        org.mockito.Mockito.doThrow(new IllegalStateException("storage unavailable"))
                .when(portfolioDeletionPortIn).deleteProjectAssets(101L);

        service.processNext();

        verify(deletionPortOut).markFailed(
                101L, deleting.getUpdatedAt(),
                ProjectErrorCode.PROJECT_DELETION_FAILED.getErrorCode(),
                ProjectErrorCode.PROJECT_DELETION_FAILED.getMessage()
        );
        verify(deletionPortOut, never()).hardDelete(any());
    }

    private Project project(ProjectStatus status, OffsetDateTime requestedAt) {
        return Project.builder()
                .id(101L)
                .userId(1L)
                .repoFullName("seungyong/helpme.md")
                .status(status)
                .deletion(requestedAt == null
                        ? ProjectDeletion.none()
                        : new ProjectDeletion(requestedAt, null))
                .build();
    }
}
