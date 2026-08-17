package seungyong.helpmebackend.activity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {
    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private ActivityPortOut activityPortOut;
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(projectAccessResolver, activityPortOut);
    }

    @Test
    void passesSearchBranchTypeAndInclusiveDateRange() {
        Project project = Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/demo").build();
        given(projectAccessResolver.resolveActive(1L, 101L)).willReturn(project);
        ActivityPage page = new ActivityPage(
                List.of(), new ActivityPage.Summary(0, 0, 0, 0), null, false, true
        );
        given(activityPortOut.findActivities(
                eq(101L), eq("webhook"), eq("main"), any(), any(), any(),
                eq(null), eq(null), eq(20), eq(true)
        )).willReturn(page);

        ActivityPage result = activityService.getActivities(
                1L, 101L, " webhook ", "main", "push_commit",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7), null, null
        );

        assertThat(result).isSameAs(page);
        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(activityPortOut).findActivities(
                eq(101L), eq("webhook"), eq("main"), any(), from.capture(), to.capture(),
                eq(null), eq(null), eq(20), eq(true)
        );
        assertThat(from.getValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(to.getValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 8));
    }

    @Test
    void rejectsInvalidTypeCursorAndSize() {
        Project project = Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/demo").build();
        given(projectAccessResolver.resolveActive(1L, 101L)).willReturn(project);

        assertBadRequest(() -> activityService.getActivities(
                1L, 101L, null, null, "commit", null, null, null, 20
        ));
        assertBadRequest(() -> activityService.getActivities(
                1L, 101L, null, null, null, null, null, "not-a-cursor", 20
        ));
        assertBadRequest(() -> activityService.getActivities(
                1L, 101L, null, null, null, null, null, null, 101
        ));
    }

    private void assertBadRequest(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);
    }
}
