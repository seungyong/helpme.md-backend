package seungyong.helpmebackend.project.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.activity.adapter.out.persistence.entity.ActivityJpaEntity;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectQueryPortOut;
import seungyong.helpmebackend.project.application.port.out.query.ProjectOverviewQuery;
import seungyong.helpmebackend.project.application.port.out.result.ProjectListQueryResult;
import seungyong.helpmebackend.project.application.port.out.result.ProjectOverviewQueryResult;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectOperationError;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;
import seungyong.helpmebackend.project.domain.type.ProjectListStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class ProjectQueryAdapterTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 30);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-30T12:00:00Z");

    @Autowired private ProjectQueryPortOut projectQueryPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private ReflectionPortOut reflectionPortOut;
    @Autowired private DevlogPortOut devlogPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private EntityManager entityManager;

    private User savedUser;
    private Project healthyProject;
    private Project warningProject;

    @BeforeEach
    void setUp() {
        savedUser = userPortOut.save(user(null, "query-token"));
        healthyProject = projectPortOut.save(project(
                "octocat/healthy", ProjectSyncStatus.READY, ProjectWebhookStatus.HEALTHY
        ));
        warningProject = projectPortOut.save(project(
                "octocat/warning", ProjectSyncStatus.FAILED, ProjectWebhookStatus.DEGRADED
        ));
        entityManager.flush();
    }

    @Test
    @DisplayName("프로젝트 페이지를 플랜 잠금·주의 상태와 실제 활동·회고 집계로 조회")
    void findProjects_aggregatesWithoutNPlusOne() {
        saveActivity(healthyProject.getId(), "commit:main:abc", "첫 활동", NOW.minusHours(1));
        saveActivity(healthyProject.getId(), "commit:main:def", "최근 활동", NOW);
        saveReflection(
                healthyProject.getId(), ReflectionKind.DAILY, TODAY, ReflectionStatus.SAVED
        );
        entityManager.flush();

        ProjectListQueryResult active = projectQueryPortOut.findProjects(
                savedUser.getId(), 1, ProjectListStatus.ACTIVE,
                NOW.minusDays(7), null, null, 20
        );
        ProjectListQueryResult attention = projectQueryPortOut.findProjects(
                savedUser.getId(), 1, ProjectListStatus.ATTENTION_REQUIRED,
                NOW.minusDays(7), null, null, 20
        );

        assertThat(active.items()).hasSize(2);
        assertThat(active.items()).filteredOn(item -> item.project().getId()
                        .equals(healthyProject.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.locked()).isFalse();
                    assertThat(item.attentionRequired()).isFalse();
                    assertThat(item.metrics().eventCount7d()).isEqualTo(2);
                    assertThat(item.metrics().completedReflectionCount()).isEqualTo(1);
                    assertThat(item.metrics().lastActivityTitle()).isEqualTo("최근 활동");
                });
        assertThat(attention.items())
                .extracting(item -> item.project().getId())
                .containsExactly(warningProject.getId());
        assertThat(attention.items().get(0).locked()).isTrue();
        assertThat(attention.items().get(0).attentionRequired()).isTrue();
    }

    @Test
    @DisplayName("개요를 activity·devlog·reflection 실제 행에서 고정 횟수로 집계")
    void findOverview_aggregatesActualRows() {
        saveActivity(healthyProject.getId(), "commit:main:abc", "오늘 활동", NOW);
        devlogPortOut.create(healthyProject.getId(), TODAY, "오늘 개발로그");
        Reflection daily = saveReflection(
                healthyProject.getId(), ReflectionKind.DAILY, TODAY, ReflectionStatus.SAVED
        );
        saveReflection(
                healthyProject.getId(), ReflectionKind.WEEKLY,
                TODAY.minusDays(6), ReflectionStatus.DRAFT
        );
        entityManager.flush();

        ProjectOverviewQueryResult result = projectQueryPortOut.findOverview(
                healthyProject.getId(),
                new ProjectOverviewQuery(
                        OffsetDateTime.parse("2026-08-17T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-24T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-31T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-30T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-31T00:00:00Z"),
                        LocalDate.of(2026, 8, 17),
                        LocalDate.of(2026, 8, 24),
                        TODAY,
                        LocalDate.of(2026, 8, 24),
                        TODAY,
                        TODAY
                ),
                5
        );

        assertThat(result.totalActivityCount()).isEqualTo(1);
        assertThat(result.currentEventCount()).isEqualTo(1);
        assertThat(result.currentCommitCount()).isEqualTo(1);
        assertThat(result.commitByBranch()).containsExactly(
                new seungyong.helpmebackend.project.domain.entity.ProjectOverview.BranchCount(
                        "main", 1
                )
        );
        assertThat(result.currentDailySavedCount()).isEqualTo(1);
        assertThat(result.currentWeeklyCount()).isEqualTo(1);
        assertThat(result.todayActivityCount()).isEqualTo(1);
        assertThat(result.devlogExists()).isTrue();
        assertThat(result.dailyReflection().id()).isEqualTo(daily.id());
        assertThat(result.recentActivities()).singleElement()
                .satisfies(activity -> assertThat(activity.title()).isEqualTo("오늘 활동"));
        assertThat(result.weekDailyReflections()).singleElement()
                .satisfies(reflection -> {
                    assertThat(reflection.date()).isEqualTo(TODAY);
                    assertThat(reflection.status()).isEqualTo(ReflectionStatus.SAVED);
                });
    }

    @Test
    @DisplayName("createdAt과 id 커서로 다음 프로젝트 페이지를 중복 없이 조회")
    void findProjects_cursorPagination() {
        ProjectListQueryResult first = projectQueryPortOut.findProjects(
                savedUser.getId(), 2, ProjectListStatus.ACTIVE,
                NOW.minusDays(7), null, null, 1
        );
        String decoded = new String(
                Base64.getUrlDecoder().decode(first.nextCursor()), StandardCharsets.UTF_8
        );
        String[] cursor = decoded.split("\\|", 2);

        ProjectListQueryResult second = projectQueryPortOut.findProjects(
                savedUser.getId(), 2, ProjectListStatus.ACTIVE,
                NOW.minusDays(7), OffsetDateTime.parse(cursor[0]), Long.parseLong(cursor[1]), 1
        );

        assertThat(first.hasNext()).isTrue();
        assertThat(first.items()).hasSize(1);
        assertThat(second.hasNext()).isFalse();
        assertThat(second.items()).hasSize(1);
        assertThat(second.items().get(0).project().getId())
                .isNotEqualTo(first.items().get(0).project().getId());
    }

    private Project project(
            String repoFullname,
            ProjectSyncStatus syncStatus,
            ProjectWebhookStatus webhookStatus
    ) {
        return Project.builder()
                .userId(savedUser.getId())
                .repoFullName(repoFullname)
                .githubRepoId(Math.abs((long) repoFullname.hashCode()))
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .sync(new ProjectSync(
                        syncStatus,
                        null,
                        null,
                        syncStatus == ProjectSyncStatus.FAILED
                                ? new ProjectOperationError(
                                "PROJECT_50001", "동기화에 실패했습니다."
                        ) : null
                ))
                .webhook(new ProjectWebhook(
                        webhookStatus,
                        null,
                        null,
                        null,
                        webhookStatus == ProjectWebhookStatus.DEGRADED
                                ? new ProjectOperationError(
                                "WEBHOOK_50001", "Webhook 처리에 실패했습니다."
                        ) : null
                ))
                .settings(ProjectSettings.defaults())
                .build();
    }

    private void saveActivity(
            Long projectId,
            String externalKey,
            String title,
            OffsetDateTime occurredAt
    ) {
        entityManager.persist(ActivityJpaEntity.builder()
                .project(entityManager.getReference(ProjectJpaEntity.class, projectId))
                .externalKey(externalKey)
                .activityType(ActivityType.PUSH_COMMIT)
                .branchName("main")
                .commitSha(externalKey.substring(externalKey.lastIndexOf(':') + 1))
                .title(title)
                .summary(title + " 요약")
                .actorLogin("octocat")
                .filesChanged(2)
                .occurredAt(occurredAt)
                .details(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode())
                .build());
    }

    private Reflection saveReflection(
            Long projectId,
            ReflectionKind kind,
            LocalDate periodStart,
            ReflectionStatus status
    ) {
        LocalDate periodEnd = kind == ReflectionKind.DAILY
                ? periodStart : periodStart.plusDays(6);
        return reflectionPortOut.createIfAbsent(Reflection.builder()
                .projectId(projectId)
                .kind(kind)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .title("회고")
                .content(ReflectionDocument.empty())
                .status(status)
                .sourceQuality(SourceQuality.COMPLETE)
                .sourceSnapshot(new ReflectionSourceSnapshot(
                        0, 0, List.of(), null, null, List.of(), 0, List.of(), false
                ))
                .generationAttempts((short) 0)
                .version(0)
                .build()).reflection();
    }
}
