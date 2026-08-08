package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.ProjectOperationError;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@Slf4j
@JpaTest
public class ProjectAdapterTest {
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectJpaRepository projectJpaRepository;

    @Test
    @DisplayName("프로젝트 저장 - 성공")
    void save_project_success() {
        User user = user(null, "test-token");

        User savedUser = userPortOut.save(user);

        Project project = project(savedUser.getId());

        Project savedProject = projectPortOut.save(project);

        assertThat(savedProject.getId()).isNotNull();
    }

    @Test
    @DisplayName("유저 ID 및 이름으로 프로젝트 조회 - 성공")
    void getByUserIdAndRepoFullName_success() {
        User user = user(null, "test-token");

        User savedUser = userPortOut.save(user);

        Project project = project(savedUser.getId());

        Project savedProject = projectPortOut.save(project);

        assertThat(savedProject.getId()).isNotNull();

        Project foundProject = projectPortOut.getByUserIdAndRepoFullName(savedUser.getId(), project.getRepoFullName())
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다."));

        assertThat(foundProject.getId()).isEqualTo(savedProject.getId());
    }

    @Test
    @DisplayName("유저 ID 및 이름으로 프로젝트 조회 - 실패")
    void getByUserIdAndRepoFullName_failure() {
        User user = user(null, "test-token");

        User savedUser = userPortOut.save(user);

        Project project = project(savedUser.getId());

        projectPortOut.save(project);

        assertThat(projectPortOut.getByUserIdAndRepoFullName(savedUser.getId(), "nonexistent/repo")).isEmpty();
    }

    @Test
    @DisplayName("현재 사용자가 이미 연결한 GitHub Repository ID만 조회")
    void getConnectedGithubRepoIds_success() {
        User savedUser = userPortOut.save(user(null, "test-token"));
        var userJpaEntity = UserPersistenceMapper.INSTANCE.toJpaEntity(savedUser);
        projectJpaRepository.save(ProjectJpaEntity.builder()
                .user(userJpaEntity)
                .repoFullName("octocat/connected")
                .githubRepoId(101L)
                .build());

        assertThat(projectPortOut.getConnectedGithubRepoIds(
                savedUser.getId(),
                java.util.List.of(101L, 102L)
        )).containsExactly(101L);
        assertThat(projectPortOut.getConnectedGithubRepoIds(
                savedUser.getId(),
                java.util.List.of()
        )).isEmpty();
    }

    @Test
    @DisplayName("프로젝트 전체 상태를 조회하고 설정만 수정")
    void getByIdAndUpdateSettings_success_preservesOperationalState() {
        User savedUser = userPortOut.save(user(null, "test-token"));
        OffsetDateTime startedAt = OffsetDateTime.of(
                2026, 8, 5, 10, 0, 0, 0, ZoneOffset.UTC
        );
        Project savedProject = projectPortOut.save(Project.builder()
                .userId(savedUser.getId())
                .repoFullName("octocat/helpme-md")
                .githubRepoId(778899L)
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .privateRepository(true)
                .sync(new ProjectSync(
                        ProjectSyncStatus.FAILED,
                        startedAt,
                        null,
                        new ProjectOperationError("PROJECT_50001", "동기화 실패")
                ))
                .webhook(new ProjectWebhook(
                        ProjectWebhookStatus.DEGRADED,
                        startedAt,
                        null,
                        "delivery-1",
                        new ProjectOperationError("WEBHOOK_50001", "Webhook 처리 실패")
                ))
                .settings(new ProjectSettings(
                        List.of("main"),
                        false,
                        "Asia/Seoul",
                        new ProjectSettings.DailyReflectionSchedule(true, LocalTime.of(23, 30)),
                        new ProjectSettings.WeeklyReflectionSchedule(
                                true, ReflectionWeekday.SUNDAY, LocalTime.of(23, 50)
                        ),
                        (short) 30
                ))
                .build());

        ProjectSettings changedSettings = new ProjectSettings(
                List.of("main", "develop"),
                false,
                "America/New_York",
                new ProjectSettings.DailyReflectionSchedule(false, LocalTime.of(22, 0)),
                new ProjectSettings.WeeklyReflectionSchedule(
                        true, ReflectionWeekday.FRIDAY, LocalTime.of(18, 30)
                ),
                (short) 14
        );
        Project updated = projectPortOut.updateSettings(savedProject.getId(), changedSettings);

        assertThat(updated.getSettings().trackedBranches()).containsExactly("main", "develop");
        assertThat(updated.getSettings().timezone()).isEqualTo("America/New_York");
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getSync().status()).isEqualTo(ProjectSyncStatus.FAILED);
        assertThat(updated.getSync().error().code()).isEqualTo("PROJECT_50001");
        assertThat(updated.getWebhook().status()).isEqualTo(ProjectWebhookStatus.DEGRADED);
        assertThat(updated.getWebhook().lastDeliveryId()).isEqualTo("delivery-1");

        Project reloaded = projectPortOut.getById(savedProject.getId()).orElseThrow();
        assertThat(reloaded.getSettings().weekly().generationDay())
                .isEqualTo(ReflectionWeekday.FRIDAY);
        assertThat(reloaded.getSettings().webhookPayloadRetentionDays()).isEqualTo((short) 14);
        assertThat(reloaded.getSync().startedAt()).isEqualTo(startedAt);
    }
}
