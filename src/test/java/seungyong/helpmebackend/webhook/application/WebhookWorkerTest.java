package seungyong.helpmebackend.webhook.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.activity.application.ActivityNormalizer;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivitySeed;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.webhook.application.port.out.InitialSyncPortOut;
import seungyong.helpmebackend.webhook.application.port.out.WebhookDeliveryPortOut;
import seungyong.helpmebackend.webhook.application.port.in.WebhookPortIn;
import seungyong.helpmebackend.webhook.application.support.WebhookActivitySeedFactory;
import seungyong.helpmebackend.webhook.application.support.WebhookTestPayloadFactory;
import seungyong.helpmebackend.webhook.domain.entity.WebhookDelivery;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@ExtendWith(MockitoExtension.class)
class WebhookWorkerTest {
    @Mock private WebhookDeliveryPortOut webhookDeliveryPortOut;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private InitialSyncPortOut initialSyncPortOut;
    @Mock private ActivityNormalizer activityNormalizer;
    @Mock private ActivityPortOut activityPortOut;
    @Mock private WebhookActivitySeedFactory webhookActivitySeedFactory;
    @Mock private UserPortOut userPortOut;
    @Mock private CipherPortOut cipherPortOut;
    @Mock private WebhookPortIn webhookPortIn;
    @Mock private WebhookTestPayloadFactory webhookTestPayloadFactory;
    private WebhookWorker worker;

    @BeforeEach
    void setUp() {
        worker = new WebhookWorker(
                webhookDeliveryPortOut, projectPortOut, initialSyncPortOut,
                activityNormalizer, activityPortOut, webhookActivitySeedFactory,
                userPortOut, cipherPortOut, webhookPortIn, webhookTestPayloadFactory
        );
    }

    @Test
    void initialSyncPersistsThroughSharedNormalizerAndMarksReady() {
        WebhookDelivery work = initialWork((short) 1);
        Project project = project();
        ActivitySeed seed = ActivitySeed.builder()
                .externalKey("commit:main:abc")
                .type(ActivityType.PUSH_COMMIT)
                .branchName("main")
                .commitSha("abc")
                .title("feat: activity")
                .occurredAt(OffsetDateTime.parse("2026-08-17T00:00:00Z"))
                .build();
        Activity activity = Activity.builder()
                .projectId(101L).externalKey(seed.externalKey()).type(seed.type())
                .title(seed.title()).occurredAt(seed.occurredAt()).build();
        given(webhookDeliveryPortOut.claimNext(any(), any())).willReturn(Optional.of(work));
        given(projectPortOut.markSyncRunning(eq(101L), any())).willReturn(project);
        given(userPortOut.getById(1L)).willReturn(user(1L));
        given(cipherPortOut.decrypt("encrypted-github-token")).willReturn("github-token");
        given(initialSyncPortOut.fetchActivities(
                eq(project), eq("github-token"), any(), eq(3)
        )).willReturn(List.of(seed));
        given(activityNormalizer.normalize(project, null, List.of(seed)))
                .willReturn(List.of(activity));

        worker.runOnce();

        verify(activityPortOut).saveAllIfAbsent(List.of(activity));
        verify(projectPortOut).markSyncReady(eq(101L), any());
        verify(webhookDeliveryPortOut).complete(eq(11L), eq(false), any());
    }

    @Test
    void thirdInitialSyncFailureBecomesTerminalProjectFailure() {
        WebhookDelivery work = initialWork((short) 3);
        Project project = project();
        given(webhookDeliveryPortOut.claimNext(any(), any())).willReturn(Optional.of(work));
        given(projectPortOut.markSyncRunning(eq(101L), any())).willReturn(project);
        given(userPortOut.getById(1L)).willReturn(user(1L));
        given(cipherPortOut.decrypt(anyString())).willReturn("github-token");
        given(initialSyncPortOut.fetchActivities(
                eq(project), eq("github-token"), any(), anyInt()
        )).willThrow(new IllegalStateException("provider payload must not leak"));

        worker.runOnce();

        verify(webhookDeliveryPortOut).fail(
                eq(11L), eq("PROJECT_50001"),
                eq("프로젝트 최초 동기화에 실패했습니다."), any(), eq(true)
        );
        verify(projectPortOut).markSyncFailed(
                eq(101L), eq("PROJECT_50001"),
                eq("프로젝트 최초 동기화에 실패했습니다."), any()
        );
    }

    private Project project() {
        return Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/demo")
                .githubRepoId(778899L).githubInstallationId(9001L)
                .defaultBranch("main").build();
    }

    private WebhookDelivery initialWork(short attempts) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-17T00:00:00Z");
        return new WebhookDelivery(
                11L, 101L, "_initial_sync", WebhookDelivery.INITIAL_SYNC_EVENT,
                null, WebhookDeliveryStatus.PROCESSING, Map.of(), null, null,
                attempts, null, now, null, null, null, now, now
        );
    }
}
