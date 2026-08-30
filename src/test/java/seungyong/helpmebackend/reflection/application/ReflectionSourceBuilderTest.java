package seungyong.helpmebackend.reflection.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivityEvidenceBatch;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReflectionSourceBuilderTest {
    private static final Long PROJECT_ID = 101L;

    @Mock private ActivityPortOut activityPortOut;
    @Mock private DevlogPortOut devlogPortOut;
    @Mock private ReflectionPortOut reflectionPortOut;
    private ReflectionSourceBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ReflectionSourceBuilder(
                activityPortOut, devlogPortOut, reflectionPortOut
        );
    }

    @Test
    @DisplayName("일일 근거는 Activity와 Devlog를 한 번씩 기간 조회하고 complete로 판정")
    void daily_complete() {
        LocalDate date = LocalDate.of(2026, 8, 30);
        given(activityPortOut.findEvidence(
                eq(PROJECT_ID), any(), any(), eq(100)
        )).willReturn(new ActivityEvidenceBatch(List.of(activity(801L, date)), 1));
        given(devlogPortOut.getByProjectIdAndLogDateBetween(
                PROJECT_ID, date, date
        )).willReturn(List.of(devlog(301L, date)));

        ReflectionSourceBuilder.Result result = builder.build(
                healthyProject(), ReflectionKind.DAILY, date, date
        );

        assertThat(result.quality()).isEqualTo(SourceQuality.COMPLETE);
        assertThat(result.snapshot().activityCount()).isEqualTo(1);
        assertThat(result.snapshot().devlogCount()).isEqualTo(1);
        assertThat(result.snapshot().evidence())
                .extracting(ReflectionSourceSnapshot.Evidence::ref)
                .containsExactly("activity:801", "devlog:301");
        assertThat(result.sourceHash()).hasSize(64);
    }

    @Test
    @DisplayName("주간 회고는 저장된 일일 회고를 우선하고 누락 날짜 Activity만 fallback")
    void weekly_partialWithFallback() {
        LocalDate start = LocalDate.of(2026, 8, 24);
        LocalDate end = start.plusDays(6);
        Activity savedDateActivity = activity(801L, start);
        Activity missingDateActivity = activity(802L, start.plusDays(1));
        given(activityPortOut.findEvidence(
                eq(PROJECT_ID), any(), any(), eq(100)
        )).willReturn(new ActivityEvidenceBatch(
                List.of(savedDateActivity, missingDateActivity), 2
        ));
        given(devlogPortOut.getByProjectIdAndLogDateBetween(
                PROJECT_ID, start, end
        )).willReturn(List.of(devlog(301L, start.plusDays(2))));
        given(reflectionPortOut.findSavedDaily(PROJECT_ID, start, end))
                .willReturn(List.of(savedDaily(401L, start)));

        ReflectionSourceBuilder.Result result = builder.build(
                healthyProject(), ReflectionKind.WEEKLY, start, end
        );

        assertThat(result.quality()).isEqualTo(SourceQuality.PARTIAL);
        assertThat(result.snapshot().expectedDailyCount()).isEqualTo(7);
        assertThat(result.snapshot().savedDailyCount()).isEqualTo(1);
        assertThat(result.snapshot().missingDailyDates()).hasSize(6);
        assertThat(result.snapshot().fallbackActivityCount()).isEqualTo(1);
        assertThat(result.snapshot().dailyReflections().get(0).reason())
                .isEqualTo("saved_reflection");
        assertThat(result.snapshot().dailyReflections().get(1).reason())
                .isEqualTo("fallback_activity");
        assertThat(result.snapshot().evidence())
                .extracting(ReflectionSourceSnapshot.Evidence::ref)
                .contains("reflection:401", "activity:802", "devlog:301")
                .doesNotContain("activity:801");
    }

    private Project healthyProject() {
        return Project.builder()
                .id(PROJECT_ID)
                .userId(1L)
                .repoFullName("octocat/helpme")
                .settings(ProjectSettings.defaults())
                .sync(new ProjectSync(ProjectSyncStatus.READY, null, null, null))
                .webhook(new ProjectWebhook(
                        ProjectWebhookStatus.HEALTHY, null, null, null, null
                ))
                .build();
    }

    private Activity activity(Long id, LocalDate date) {
        return Activity.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .externalKey("commit:" + id)
                .type(ActivityType.PUSH_COMMIT)
                .branchName("main")
                .commitSha("abcdef123456")
                .title("feat: 회고")
                .summary("회고 기능 구현")
                .occurredAt(date.atTime(12, 0).atOffset(java.time.ZoneOffset.UTC))
                .build();
    }

    private Devlog devlog(Long id, LocalDate date) {
        return new Devlog(
                id, PROJECT_ID, date, "설계 결정을 기록함", 0,
                OffsetDateTime.parse("2026-08-30T00:00:00Z"),
                OffsetDateTime.parse("2026-08-30T00:00:00Z")
        );
    }

    private Reflection savedDaily(Long id, LocalDate date) {
        return Reflection.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .kind(ReflectionKind.DAILY)
                .periodStart(date)
                .periodEnd(date)
                .title("저장된 일일 회고")
                .content(new ReflectionDocument(1, List.of(
                        new ReflectionDocument.Section(
                                "summary", "markdown", "요약", "완료한 작업", List.of()
                        )
                )))
                .status(ReflectionStatus.SAVED)
                .sourceQuality(SourceQuality.COMPLETE)
                .sourceSnapshot(ReflectionSourceSnapshot.empty())
                .generationAttempts((short) 1)
                .version(1)
                .build();
    }
}
