package seungyong.helpmebackend.project.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.project.application.port.in.ProjectPortIn;
import seungyong.helpmebackend.project.application.port.in.command.UpdateProjectSettingsCommand;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectOperationError;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ProjectController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class ProjectControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProjectPortIn projectPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Nested
    @DisplayName("프로젝트 상세 조회")
    class GetProject {
        @Test
        @DisplayName("canonical 프로젝트와 sync·Webhook 상태를 반환")
        void success() throws Exception {
            given(projectPortIn.getProject(USER_ID, PROJECT_ID)).willReturn(project());

            mockMvc.perform(get("/api/v1/projects/{projectId}", PROJECT_ID).with(user()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PROJECT_ID))
                    .andExpect(jsonPath("$.repoFullname").value("seungyong/helpme.md"))
                    .andExpect(jsonPath("$.githubRepoId").value(778899L))
                    .andExpect(jsonPath("$.defaultBranch").value("main"))
                    .andExpect(jsonPath("$.trackedBranches[1]").value("test"))
                    .andExpect(jsonPath("$.trackAllBranches").value(false))
                    .andExpect(jsonPath("$.isPrivate").value(true))
                    .andExpect(jsonPath("$.status").value("active"))
                    .andExpect(jsonPath("$.sync.status").value("failed"))
                    .andExpect(jsonPath("$.sync.error.code").value("PROJECT_50001"))
                    .andExpect(jsonPath("$.sync.error.retryable").value(true))
                    .andExpect(jsonPath("$.webhook.status").value("healthy"))
                    .andExpect(jsonPath("$.webhook.error").doesNotExist())
                    .andExpect(jsonPath("$.timezone").value("Asia/Seoul"));
        }
    }

    @Nested
    @DisplayName("프로젝트 설정 조회")
    class GetSettings {
        @Test
        @DisplayName("Branch·timezone·회고 일정·보관 기간과 건강 상태를 반환")
        void success() throws Exception {
            given(projectPortIn.getProjectSettings(USER_ID, PROJECT_ID)).willReturn(project());

            mockMvc.perform(get("/api/v1/projects/{projectId}/settings", PROJECT_ID).with(user()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trackedBranches[0]").value("main"))
                    .andExpect(jsonPath("$.daily.enabled").value(true))
                    .andExpect(jsonPath("$.daily.generationTime").value("23:30"))
                    .andExpect(jsonPath("$.weekly.generationDay").value("sunday"))
                    .andExpect(jsonPath("$.weekly.generationTime").value("23:50"))
                    .andExpect(jsonPath("$.webhookPayloadRetentionDays").value(30))
                    .andExpect(jsonPath("$.sync.status").value("failed"))
                    .andExpect(jsonPath("$.webhook.status").value("healthy"))
                    .andExpect(jsonPath("$.status").value("active"));
        }
    }

    @Nested
    @DisplayName("프로젝트 설정 수정")
    class UpdateSettings {
        @Test
        @DisplayName("부분 수정 요청을 command로 변환하고 저장된 최신 설정을 반환")
        void success() throws Exception {
            Project changed = projectWithSettings(new ProjectSettings(
                    List.of("main", "develop"),
                    false,
                    "America/New_York",
                    new ProjectSettings.DailyReflectionSchedule(false, LocalTime.of(22, 0)),
                    new ProjectSettings.WeeklyReflectionSchedule(
                            true, ReflectionWeekday.FRIDAY, LocalTime.of(18, 30)
                    ),
                    (short) 14
            ));
            given(projectPortIn.updateProjectSettings(
                    org.mockito.ArgumentMatchers.any(UpdateProjectSettingsCommand.class)
            )).willReturn(changed);

            mockMvc.perform(patch("/api/v1/projects/{projectId}/settings", PROJECT_ID)
                            .with(user())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "trackedBranches":["main","develop"],
                                      "trackAllBranches":false,
                                      "timezone":"America/New_York",
                                      "dailyEnabled":false,
                                      "dailyGenerationTime":"22:00",
                                      "weeklyEnabled":true,
                                      "weeklyGenerationDay":"friday",
                                      "weeklyGenerationTime":"18:30",
                                      "webhookPayloadRetentionDays":14
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trackedBranches[1]").value("develop"))
                    .andExpect(jsonPath("$.timezone").value("America/New_York"))
                    .andExpect(jsonPath("$.daily.enabled").value(false))
                    .andExpect(jsonPath("$.daily.generationTime").value("22:00"))
                    .andExpect(jsonPath("$.weekly.generationDay").value("friday"))
                    .andExpect(jsonPath("$.webhookPayloadRetentionDays").value(14))
                    .andExpect(jsonPath("$.updatedAt").value("2026-08-05T13:00:00Z"))
                    .andExpect(jsonPath("$.sync").doesNotExist())
                    .andExpect(jsonPath("$.webhook").doesNotExist());

            ArgumentCaptor<UpdateProjectSettingsCommand> captor =
                    ArgumentCaptor.forClass(UpdateProjectSettingsCommand.class);
            verify(projectPortIn).updateProjectSettings(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().projectId()).isEqualTo(PROJECT_ID);
            assertThat(captor.getValue().dailyGenerationTime()).isEqualTo(LocalTime.of(22, 0));
            assertThat(captor.getValue().weeklyGenerationDay())
                    .isEqualTo(ReflectionWeekday.FRIDAY);
        }

        @Test
        @DisplayName("시간·요일·보관 기간 형식이 잘못되면 400")
        void failure_invalidRequest() throws Exception {
            mockMvc.perform(patch("/api/v1/projects/{projectId}/settings", PROJECT_ID)
                            .with(user())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "dailyGenerationTime":"24:00",
                                      "weeklyGenerationDay":"funday",
                                      "webhookPayloadRetentionDays":31
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("REQ_400"));

            verify(projectPortIn, never()).updateProjectSettings(
                    org.mockito.ArgumentMatchers.any(UpdateProjectSettingsCommand.class)
            );
        }

        @Test
        @DisplayName("요일의 기존 숫자 표현은 400")
        void failure_numericWeekday() throws Exception {
            mockMvc.perform(patch("/api/v1/projects/{projectId}/settings", PROJECT_ID)
                            .with(user())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "weeklyGenerationDay":5
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("REQ_400"));

            verify(projectPortIn, never()).updateProjectSettings(
                    org.mockito.ArgumentMatchers.any(UpdateProjectSettingsCommand.class)
            );
        }
    }

    private Project project() {
        return Project.builder()
                .id(PROJECT_ID)
                .userId(USER_ID)
                .repoFullName("seungyong/helpme.md")
                .githubRepoId(778899L)
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .privateRepository(true)
                .status(ProjectStatus.ACTIVE)
                .sync(new ProjectSync(
                        ProjectSyncStatus.FAILED,
                        OffsetDateTime.of(2026, 8, 5, 11, 0, 0, 0, ZoneOffset.UTC),
                        null,
                        new ProjectOperationError("PROJECT_50001", "GitHub 동기화에 실패했습니다.")
                ))
                .webhook(new ProjectWebhook(
                        ProjectWebhookStatus.HEALTHY,
                        OffsetDateTime.of(2026, 8, 5, 11, 30, 0, 0, ZoneOffset.UTC),
                        OffsetDateTime.of(2026, 8, 5, 11, 29, 0, 0, ZoneOffset.UTC),
                        "delivery-1",
                        null
                ))
                .settings(settings())
                .updatedAt(OffsetDateTime.of(2026, 8, 5, 13, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    private Project projectWithSettings(ProjectSettings changedSettings) {
        Project project = project();
        project.changeSettings(changedSettings);
        return project;
    }

    private ProjectSettings settings() {
        return new ProjectSettings(
                List.of("main", "test"),
                false,
                "Asia/Seoul",
                new ProjectSettings.DailyReflectionSchedule(true, LocalTime.of(23, 30)),
                new ProjectSettings.WeeklyReflectionSchedule(
                        true, ReflectionWeekday.SUNDAY, LocalTime.of(23, 50)
                ),
                (short) 30
        );
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(USER_ID, "seungyong")
        );
    }
}
