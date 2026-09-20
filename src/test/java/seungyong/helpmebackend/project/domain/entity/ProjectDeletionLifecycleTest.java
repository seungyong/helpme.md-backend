package seungyong.helpmebackend.project.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDeletionLifecycleTest {
    @Test
    @DisplayName("프로젝트 정리 실패는 최초 requestedAt을 유지한 delete_failed 상태")
    void recordsFailureWithoutLosingRequest() {
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T09:00:00Z");
        Project project = Project.builder()
                .id(101L)
                .userId(1L)
                .repoFullName("seungyong/helpme.md")
                .build();
        project.requestDeletion(requestedAt);

        project.recordDeletionFailure(
                new ProjectOperationError("PROJECT_50002", "cleanup failed")
        );

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DELETE_FAILED);
        assertThat(project.getDeletion().requestedAt()).isEqualTo(requestedAt);
        assertThat(project.getDeletion().error().code()).isEqualTo("PROJECT_50002");
    }
}
