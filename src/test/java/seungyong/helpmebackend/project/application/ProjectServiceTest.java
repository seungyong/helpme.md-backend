package seungyong.helpmebackend.project.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.github.application.port.in.GithubRepositoryAccessPortIn;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.in.command.UpdateProjectSettingsCommand;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;

    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private GithubRepositoryAccessPortIn githubRepositoryAccessPortIn;
    @InjectMocks private ProjectService projectService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder()
                .id(PROJECT_ID)
                .userId(USER_ID)
                .repoFullName("seungyong/helpme.md")
                .githubRepoId(778899L)
                .githubInstallationId(9001L)
                .settings(ProjectSettings.defaults())
                .updatedAt(OffsetDateTime.of(2026, 8, 5, 12, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    @Nested
    @DisplayName("프로젝트 조회")
    class GetProject {
        @Test
        @DisplayName("상세와 설정 조회 모두 공통 active resolver를 사용")
        void success() {
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);

            assertThat(projectService.getProject(USER_ID, PROJECT_ID)).isSameAs(project);
            assertThat(projectService.getProjectSettings(USER_ID, PROJECT_ID)).isSameAs(project);

            verify(projectAccessResolver, org.mockito.Mockito.times(2))
                    .resolveActive(USER_ID, PROJECT_ID);
        }
    }

    @Nested
    @DisplayName("프로젝트 설정 수정")
    class UpdateSettings {
        @Test
        @DisplayName("전체 설정을 병합하고 선택 Branch를 GitHub에서 검증한 뒤 저장")
        void success() {
            UpdateProjectSettingsCommand command = command(
                    List.of("main", "develop"),
                    false,
                    "America/New_York",
                    false,
                    LocalTime.of(22, 0),
                    true,
                    ReflectionWeekday.FRIDAY,
                    LocalTime.of(18, 30),
                    (short) 14
            );
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);
            given(projectPortOut.updateSettings(
                    org.mockito.ArgumentMatchers.eq(PROJECT_ID),
                    org.mockito.ArgumentMatchers.any(ProjectSettings.class)
            ))
                    .willAnswer(invocation -> project);

            Project result = projectService.updateProjectSettings(command);

            assertThat(result.getSettings().trackedBranches()).containsExactly("main", "develop");
            assertThat(result.getSettings().timezone()).isEqualTo("America/New_York");
            assertThat(result.getSettings().daily().enabled()).isFalse();
            assertThat(result.getSettings().weekly().generationDay())
                    .isEqualTo(ReflectionWeekday.FRIDAY);
            assertThat(result.getSettings().webhookPayloadRetentionDays()).isEqualTo((short) 14);
            verify(githubRepositoryAccessPortIn).validateRepositoryBranches(
                    USER_ID,
                    9001L,
                    778899L,
                    "seungyong/helpme.md",
                    Set.of("main", "develop")
            );
            verify(projectPortOut).updateSettings(PROJECT_ID, result.getSettings());
        }

        @Test
        @DisplayName("일정만 수정하면 GitHub를 호출하지 않고 기존 설정을 보존")
        void success_partialSchedule() {
            UpdateProjectSettingsCommand command = command(
                    null, null, null, false, null, null, null, null, null
            );
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);
            given(projectPortOut.updateSettings(
                    org.mockito.ArgumentMatchers.eq(PROJECT_ID),
                    org.mockito.ArgumentMatchers.any(ProjectSettings.class)
            ))
                    .willAnswer(invocation -> project);

            Project result = projectService.updateProjectSettings(command);

            assertThat(result.getSettings().daily().enabled()).isFalse();
            assertThat(result.getSettings().daily().generationTime()).isEqualTo(LocalTime.of(23, 30));
            verify(githubRepositoryAccessPortIn, never())
                    .validateRepositoryBranches(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.anySet()
                    );
        }

        @Test
        @DisplayName("전체 Branch 수집으로 바꾸면 선택 목록을 비우고 GitHub를 호출하지 않음")
        void success_trackAllBranches() {
            UpdateProjectSettingsCommand command = command(
                    List.of("main"), true, null, null, null, null, null, null, null
            );
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);
            given(projectPortOut.updateSettings(
                    org.mockito.ArgumentMatchers.eq(PROJECT_ID),
                    org.mockito.ArgumentMatchers.any(ProjectSettings.class)
            ))
                    .willAnswer(invocation -> project);

            Project result = projectService.updateProjectSettings(command);

            assertThat(result.getSettings().trackAllBranches()).isTrue();
            assertThat(result.getSettings().trackedBranches()).isEmpty();
            verify(githubRepositoryAccessPortIn, never())
                    .validateRepositoryBranches(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.anySet()
                    );
        }

        @Test
        @DisplayName("빈 PATCH는 저장하거나 GitHub를 호출하지 않음")
        void success_noChanges() {
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);

            Project result = projectService.updateProjectSettings(command(
                    null, null, null, null, null, null, null, null, null
            ));

            assertThat(result).isSameAs(project);
            verify(projectPortOut, never()).updateSettings(
                    org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()
            );
            verify(githubRepositoryAccessPortIn, never())
                    .validateRepositoryBranches(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.anySet()
                    );
        }

        @Test
        @DisplayName("동일한 Branch 필드를 포함해 다른 설정만 수정하면 GitHub를 호출하지 않음")
        void success_sameBranchConfiguration() {
            UpdateProjectSettingsCommand command = command(
                    List.of(), false, "America/New_York", null, null, null, null, null, null
            );
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);
            given(projectPortOut.updateSettings(
                    org.mockito.ArgumentMatchers.eq(PROJECT_ID),
                    org.mockito.ArgumentMatchers.any(ProjectSettings.class)
            )).willAnswer(invocation -> project);

            Project result = projectService.updateProjectSettings(command);

            assertThat(result.getSettings().timezone()).isEqualTo("America/New_York");
            verify(githubRepositoryAccessPortIn, never()).validateRepositoryBranches(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anySet()
            );
        }

        @Test
        @DisplayName("모든 설정 값이 동일하면 GitHub와 DB를 호출하지 않음")
        void success_sameSettings() {
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);

            Project result = projectService.updateProjectSettings(command(
                    List.of(),
                    false,
                    "Asia/Seoul",
                    true,
                    LocalTime.of(23, 30),
                    true,
                    ReflectionWeekday.SUNDAY,
                    LocalTime.of(23, 50),
                    (short) 30
            ));

            assertThat(result).isSameAs(project);
            verify(projectPortOut, never()).updateSettings(
                    org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()
            );
            verify(githubRepositoryAccessPortIn, never()).validateRepositoryBranches(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anySet()
            );
        }

        @Test
        @DisplayName("IANA timezone과 API payload 보관 범위를 검증")
        void failure_invalidValues() {
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);

            assertBadRequest(command(
                    null, null, "+09:00", null, null, null, null, null, null
            ));
            assertBadRequest(command(
                    null, null, null, null, null, null, null, null, (short) 6
            ));
        }

        @Test
        @DisplayName("선택 Branch 중 하나라도 GitHub에 없으면 404")
        void failure_branchNotFound() {
            given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project);
            willThrow(new CustomException(GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND))
                    .given(githubRepositoryAccessPortIn)
                    .validateRepositoryBranches(
                            USER_ID,
                            9001L,
                            778899L,
                            "seungyong/helpme.md",
                            Set.of("main", "deleted")
                    );

            assertThatThrownBy(() -> projectService.updateProjectSettings(command(
                    List.of("main", "deleted"), false, null, null, null, null, null, null, null
            ))).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND
                    );

            verify(projectPortOut, never()).updateSettings(
                    org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()
            );
        }

        private void assertBadRequest(UpdateProjectSettingsCommand command) {
            assertThatThrownBy(() -> projectService.updateProjectSettings(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);
        }
    }

    private UpdateProjectSettingsCommand command(
            List<String> trackedBranches,
            Boolean trackAllBranches,
            String timezone,
            Boolean dailyEnabled,
            LocalTime dailyGenerationTime,
            Boolean weeklyEnabled,
            ReflectionWeekday weeklyGenerationDay,
            LocalTime weeklyGenerationTime,
            Short retentionDays
    ) {
        return new UpdateProjectSettingsCommand(
                USER_ID,
                PROJECT_ID,
                trackedBranches,
                trackAllBranches,
                timezone,
                dailyEnabled,
                dailyGenerationTime,
                weeklyEnabled,
                weeklyGenerationDay,
                weeklyGenerationTime,
                retentionDays
        );
    }
}
