package seungyong.helpmebackend.project.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectQueryPortOut;
import seungyong.helpmebackend.project.application.port.out.result.ProjectListQueryResult;
import seungyong.helpmebackend.project.application.port.out.result.ProjectOverviewQueryResult;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectList;
import seungyong.helpmebackend.project.domain.entity.ProjectOverview;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;
import seungyong.helpmebackend.project.domain.type.ProjectHealthStatus;
import seungyong.helpmebackend.project.domain.type.ProjectListStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;

    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private ProjectQueryPortOut projectQueryPortOut;
    @Mock private UserPortOut userPortOut;

    private ProjectQueryService service;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new ProjectQueryService(
                projectAccessResolver, projectPortOut, projectQueryPortOut, userPortOut
        );
        project = Project.builder()
                .id(PROJECT_ID)
                .userId(USER_ID)
                .repoFullName("seungyong/helpme.md")
                .defaultBranch("main")
                .sync(new ProjectSync(ProjectSyncStatus.READY, null, null, null))
                .webhook(new ProjectWebhook(
                        ProjectWebhookStatus.HEALTHY, null, null, null, null
                ))
                .settings(ProjectSettings.defaults())
                .build();
    }

    @Nested
    @DisplayName("프로젝트 목록")
    class ListProjects {
        @Test
        @DisplayName("기본 size와 active 필터로 플랜·페이지·집계를 반환")
        void success_default() {
            ProjectList.Item item = new ProjectList.Item(
                    project, false, false, new ProjectList.Metrics(3, 1, "최근 활동")
            );
            given(userPortOut.getById(USER_ID)).willReturn(user(USER_ID));
            given(projectPortOut.countByUserId(USER_ID)).willReturn(1L);
            given(projectQueryPortOut.findProjects(
                    anyLong(), anyInt(), any(), any(), any(), any(), anyInt()
            )).willReturn(new ProjectListQueryResult(
                    List.of(item), "next", true
            ));

            ProjectList result = service.getProjects(USER_ID, null, null, null);

            assertThat(result.plan()).isEqualTo(new ProjectList.Plan("free", 1, 1));
            assertThat(result.items()).containsExactly(item);
            assertThat(result.page()).isEqualTo(new ProjectList.Page("next", true));
            verify(projectQueryPortOut).findProjects(
                    org.mockito.ArgumentMatchers.eq(USER_ID),
                    org.mockito.ArgumentMatchers.eq(1),
                    org.mockito.ArgumentMatchers.eq(ProjectListStatus.ACTIVE),
                    any(),
                    org.mockito.ArgumentMatchers.isNull(),
                    org.mockito.ArgumentMatchers.isNull(),
                    org.mockito.ArgumentMatchers.eq(20)
            );
        }

        @Test
        @DisplayName("attention_required 필터와 cursor를 해석")
        void success_attentionCursor() {
            OffsetDateTime cursorTime = OffsetDateTime.parse("2026-08-30T10:00:00Z");
            String cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (cursorTime + "|101").getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            given(userPortOut.getById(USER_ID)).willReturn(user(USER_ID));
            given(projectQueryPortOut.findProjects(
                    anyLong(), anyInt(), any(), any(), any(), any(), anyInt()
            )).willReturn(new ProjectListQueryResult(List.of(), null, false));

            service.getProjects(USER_ID, cursor, 10, "attention_required");

            ArgumentCaptor<OffsetDateTime> timeCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            verify(projectQueryPortOut).findProjects(
                    org.mockito.ArgumentMatchers.eq(USER_ID),
                    org.mockito.ArgumentMatchers.eq(1),
                    org.mockito.ArgumentMatchers.eq(ProjectListStatus.ATTENTION_REQUIRED),
                    any(),
                    timeCaptor.capture(),
                    org.mockito.ArgumentMatchers.eq(101L),
                    org.mockito.ArgumentMatchers.eq(10)
            );
            assertThat(timeCaptor.getValue()).isEqualTo(cursorTime);
        }

        @Test
        @DisplayName("잘못된 size, status, cursor는 400")
        void failure_invalidQuery() {
            given(userPortOut.getById(USER_ID)).willReturn(user(USER_ID));

            assertBadRequest(() -> service.getProjects(USER_ID, null, 0, null));
            assertBadRequest(() -> service.getProjects(USER_ID, null, 20, "deleted"));
            assertBadRequest(() -> service.getProjects(USER_ID, "broken", 20, null));
        }
    }

    @Nested
    @DisplayName("프로젝트 개요")
    class Overview {
        @Test
        @DisplayName("실제 집계값으로 warning과 증감률·오늘·주간 상태를 조합")
        void success() {
            Project warningProject = Project.builder()
                    .id(PROJECT_ID)
                    .userId(USER_ID)
                    .repoFullName("seungyong/helpme.md")
                    .defaultBranch("main")
                    .sync(new ProjectSync(ProjectSyncStatus.FAILED, null, null, null))
                    .webhook(new ProjectWebhook(
                            ProjectWebhookStatus.DEGRADED, null, null, null, null
                    ))
                    .settings(ProjectSettings.defaults())
                    .build();
            ProjectOverviewQueryResult queryResult = new ProjectOverviewQueryResult(
                    5, 4, 2, 3, 1,
                    List.of(new ProjectOverview.BranchCount("main", 3)),
                    5, 4, 1, 1, 2, true,
                    new ProjectOverview.DailyReflection(401L, ReflectionStatus.DRAFT),
                    List.of(new ProjectOverview.RecentActivity(
                            801L, ActivityType.PUSH_COMMIT, "활동", "요약", "main",
                            "abc", 2, OffsetDateTime.parse("2026-08-30T10:00:00Z")
                    )),
                    List.of(new ProjectOverview.Daily(
                            LocalDate.of(2026, 8, 30), ReflectionStatus.SAVED, 400L
                    ))
            );
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID))
                    .willReturn(warningProject);
            given(projectQueryPortOut.findOverview(
                    org.mockito.ArgumentMatchers.eq(PROJECT_ID), any(), anyInt()
            )).willReturn(queryResult);

            ProjectOverview result = service.getOverview(USER_ID, PROJECT_ID);

            assertThat(result.healthStatus()).isEqualTo(ProjectHealthStatus.WARNING);
            assertThat(result.metrics().events7d().changeRate()).isEqualTo(100.0);
            assertThat(result.metrics().commits7d().changeRate()).isEqualTo(200.0);
            assertThat(result.today().devlogExists()).isTrue();
            assertThat(result.currentWeek().completedDailyCount()).isEqualTo(1);
            assertThat(result.nextGeneration().dailyAt()).isNotNull();
            assertThat(result.nextGeneration().weeklyAt()).isNotNull();
        }

        @Test
        @DisplayName("동기화 완료 후 활동이 없으면 no_events")
        void success_noEvents() {
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);
            given(projectQueryPortOut.findOverview(
                    org.mockito.ArgumentMatchers.eq(PROJECT_ID), any(), anyInt()
            )).willReturn(new ProjectOverviewQueryResult(
                    0, 0, 0, 0, 0, List.of(),
                    0, 0, 0, 0, 0, false,
                    null, List.of(), List.of()
            ));

            ProjectOverview result = service.getOverview(USER_ID, PROJECT_ID);

            assertThat(result.healthStatus()).isEqualTo(ProjectHealthStatus.NO_EVENTS);
        }
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);
    }
}
