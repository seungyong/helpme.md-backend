package seungyong.helpmebackend.project.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.project.application.port.in.ProjectQueryPortIn;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectList;
import seungyong.helpmebackend.project.domain.entity.ProjectOverview;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;
import seungyong.helpmebackend.project.domain.type.ProjectHealthStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ProjectQueryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class ProjectQueryControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProjectQueryPortIn projectQueryPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("프로젝트 목록의 플랜·상태·집계·페이지를 계약대로 반환")
    void getProjects_success() throws Exception {
        Project project = project();
        given(projectQueryPortIn.getProjects(USER_ID, "cursor", 10, "attention_required"))
                .willReturn(new ProjectList(
                        new ProjectList.Plan("free", 1, 2),
                        List.of(new ProjectList.Item(
                                project, true, true,
                                new ProjectList.Metrics(42, 9, "Webhook 설정 UX 개선")
                        )),
                        new ProjectList.Page("next", true)
                ));

        mockMvc.perform(get("/api/v1/projects")
                        .with(user())
                        .param("cursor", "cursor")
                        .param("size", "10")
                        .param("status", "attention_required"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("free"))
                .andExpect(jsonPath("$.plan.limit").value(1))
                .andExpect(jsonPath("$.plan.used").value(2))
                .andExpect(jsonPath("$.items[0].id").value(PROJECT_ID))
                .andExpect(jsonPath("$.items[0].syncStatus").value("ready"))
                .andExpect(jsonPath("$.items[0].webhookStatus").value("healthy"))
                .andExpect(jsonPath("$.items[0].isLocked").value(true))
                .andExpect(jsonPath("$.items[0].attentionRequired").value(true))
                .andExpect(jsonPath("$.items[0].metrics.eventCount7d").value(42))
                .andExpect(jsonPath("$.items[0].metrics.lastActivityTitle")
                        .value("Webhook 설정 UX 개선"))
                .andExpect(jsonPath("$.page.nextCursor").value("next"))
                .andExpect(jsonPath("$.page.hasNext").value(true));

        verify(projectQueryPortIn).getProjects(
                USER_ID, "cursor", 10, "attention_required"
        );
    }

    @Test
    @DisplayName("프로젝트 개요의 건강 상태와 실제 집계를 계약대로 반환")
    void getOverview_success() throws Exception {
        LocalDate today = LocalDate.of(2026, 8, 30);
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-30T05:22:00Z");
        ProjectOverview result = new ProjectOverview(
                ProjectHealthStatus.HEALTHY,
                project(),
                new ProjectOverview.Metrics(
                        new ProjectOverview.Comparison(42, 30, 40.0),
                        new ProjectOverview.CommitComparison(
                                18, 12, 50.0,
                                List.of(new ProjectOverview.BranchCount("main", 18))
                        ),
                        new ProjectOverview.Comparison(5, 4, 25.0),
                        new ProjectOverview.Comparison(1, 1, 0.0)
                ),
                new ProjectOverview.Today(
                        today, 12, true,
                        new ProjectOverview.DailyReflection(401L, ReflectionStatus.DRAFT)
                ),
                List.of(new ProjectOverview.RecentActivity(
                        801L, ActivityType.PUSH_COMMIT, "Webhook 설정 UX 개선",
                        "수신 실패 복구 흐름", "main", "a32f91d", 4, occurredAt
                )),
                new ProjectOverview.CurrentWeek(
                        today.minusDays(6), today, 1,
                        List.of(new ProjectOverview.Daily(
                                today, ReflectionStatus.SAVED, 401L
                        ))
                ),
                new ProjectOverview.NextGeneration(
                        OffsetDateTime.parse("2026-08-30T14:30:00Z"),
                        OffsetDateTime.parse("2026-08-30T14:50:00Z")
                )
        );
        given(projectQueryPortIn.getOverview(USER_ID, PROJECT_ID)).willReturn(result);

        mockMvc.perform(get("/api/v1/projects/{projectId}/overview", PROJECT_ID).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthStatus").value("healthy"))
                .andExpect(jsonPath("$.project.id").value(PROJECT_ID))
                .andExpect(jsonPath("$.sync.status").value("ready"))
                .andExpect(jsonPath("$.sync.error").doesNotExist())
                .andExpect(jsonPath("$.webhook.status").value("healthy"))
                .andExpect(jsonPath("$.metrics.events7d.current").value(42))
                .andExpect(jsonPath("$.metrics.commits7d.byBranch[0].branch").value("main"))
                .andExpect(jsonPath("$.today.activityCount").value(12))
                .andExpect(jsonPath("$.today.devlog.exists").value(true))
                .andExpect(jsonPath("$.today.dailyReflection.id").value(401))
                .andExpect(jsonPath("$.recentActivities[0].activityType")
                        .value("push_commit"))
                .andExpect(jsonPath("$.currentWeek.completedDailyCount").value(1))
                .andExpect(jsonPath("$.nextGeneration.dailyAt")
                        .value("2026-08-30T14:30:00Z"));
    }

    private Project project() {
        return Project.builder()
                .id(PROJECT_ID)
                .userId(USER_ID)
                .repoFullName("seungyong/helpme.md")
                .defaultBranch("main")
                .sync(new ProjectSync(ProjectSyncStatus.READY, null, null, null))
                .webhook(new ProjectWebhook(
                        ProjectWebhookStatus.HEALTHY,
                        OffsetDateTime.parse("2026-08-30T05:27:00Z"),
                        OffsetDateTime.parse("2026-08-30T05:27:00Z"),
                        "delivery-1",
                        null
                ))
                .settings(ProjectSettings.defaults())
                .build();
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(USER_ID, "seungyong")
        );
    }
}
