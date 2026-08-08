package seungyong.helpmebackend.project.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProjectAccessResolverTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;

    @Mock private ProjectPortOut projectPortOut;
    @InjectMocks private ProjectAccessResolver projectAccessResolver;

    @Nested
    @DisplayName("활성 프로젝트 접근")
    class ResolveActive {
        @Test
        @DisplayName("소유한 활성 프로젝트를 반환")
        void success() {
            Project project = project(USER_ID, ProjectStatus.ACTIVE);
            given(projectPortOut.getById(PROJECT_ID)).willReturn(Optional.of(project));

            assertThat(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).isSameAs(project);
        }

        @Test
        @DisplayName("프로젝트가 없으면 404")
        void failure_notFound() {
            given(projectPortOut.getById(PROJECT_ID)).willReturn(Optional.empty());

            assertError(PROJECT_ID, ProjectErrorCode.PROJECT_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사용자의 프로젝트면 403")
        void failure_accessDenied() {
            given(projectPortOut.getById(PROJECT_ID))
                    .willReturn(Optional.of(project(2L, ProjectStatus.ACTIVE)));

            assertError(PROJECT_ID, ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("deleting 또는 delete_failed 프로젝트면 409")
        void failure_notActive() {
            given(projectPortOut.getById(PROJECT_ID))
                    .willReturn(Optional.of(project(USER_ID, ProjectStatus.DELETE_FAILED)));

            assertError(PROJECT_ID, ProjectErrorCode.PROJECT_NOT_ACTIVE);
        }
    }

    private void assertError(Long projectId, ProjectErrorCode errorCode) {
        assertThatThrownBy(() -> projectAccessResolver.resolveActive(USER_ID, projectId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private Project project(Long userId, ProjectStatus status) {
        return Project.builder()
                .id(PROJECT_ID)
                .userId(userId)
                .repoFullName("seungyong/helpme.md")
                .status(status)
                .build();
    }
}
