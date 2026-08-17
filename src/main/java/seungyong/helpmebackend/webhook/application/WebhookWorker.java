package seungyong.helpmebackend.webhook.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.activity.application.ActivityNormalizer;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivitySeed;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.webhook.application.port.out.InitialSyncPortOut;
import seungyong.helpmebackend.webhook.application.port.out.WebhookDeliveryPortOut;
import seungyong.helpmebackend.webhook.application.port.in.WebhookPortIn;
import seungyong.helpmebackend.webhook.application.support.WebhookActivitySeedFactory;
import seungyong.helpmebackend.webhook.application.support.WebhookTestPayloadFactory;
import seungyong.helpmebackend.webhook.domain.entity.WebhookDelivery;
import seungyong.helpmebackend.webhook.domain.exception.WebhookErrorCode;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookWorker {
    private static final Duration STUCK_AFTER = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 3;
    private static final int INITIAL_SYNC_DAYS = 30;
    private static final int INITIAL_SYNC_MAX_PAGES = 3;

    private final WebhookDeliveryPortOut webhookDeliveryPortOut;
    private final ProjectPortOut projectPortOut;
    private final InitialSyncPortOut initialSyncPortOut;
    private final ActivityNormalizer activityNormalizer;
    private final ActivityPortOut activityPortOut;
    private final WebhookActivitySeedFactory webhookActivitySeedFactory;
    private final UserPortOut userPortOut;
    private final CipherPortOut cipherPortOut;
    private final WebhookPortIn webhookPortIn;
    private final WebhookTestPayloadFactory webhookTestPayloadFactory;

    /**
     * WebhookDelivery를 처리하는 작업을 주기적으로 실행합니다.
     * <pre>
     * - 처리할 WebhookDelivery가 존재하면, 하나씩 처리합니다.
     * - 처리 중 예외가 발생하면, 실패 횟수를 증가시키고 재시도 여부를 결정합니다.
     * - 재시도 횟수가 최대치를 초과하면, 해당 WebhookDelivery를 실패 상태로 마킹합니다.
     * </pre>
     */
    @Scheduled(fixedDelayString = "${workers.webhook.fixed-delay-ms:1000}")
    public void runOnce() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        webhookDeliveryPortOut.claimNext(now, now.minus(STUCK_AFTER))
                .ifPresent(this::process);
    }

    /**
     * WebhookDelivery 중, 처리되지 않은 테스트(Webhook Test)들을 만료시키고,
     * 만료된 페이로드를 정리하는 작업을 주기적으로 실행합니다.
     */
    @Scheduled(fixedDelayString = "${workers.webhook.maintenance-delay-ms:60000}")
    public void runMaintenance() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        webhookDeliveryPortOut.expireTimedOutTests(
                now,
                WebhookErrorCode.WEBHOOK_TEST_TIMEOUT.getErrorCode(),
                WebhookErrorCode.WEBHOOK_TEST_TIMEOUT.getMessage()
        );
        webhookDeliveryPortOut.purgeExpiredPayloads(now);
    }

    private void process(WebhookDelivery delivery) {
        try {
            if (delivery.isInitialSync()) {
                processInitialSync(delivery);
            } else if (delivery.isWebhookTest()) {
                processWebhookTest(delivery);
            } else {
                processGithubDelivery(delivery);
            }
        } catch (RuntimeException exception) {
            handleFailure(delivery, exception);
        }
    }

    private void processInitialSync(WebhookDelivery delivery) {
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        Project project = projectPortOut.markSyncRunning(delivery.projectId(), startedAt);
        User user = userPortOut.getById(project.getUserId());
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken().value());
        List<ActivitySeed> seeds = initialSyncPortOut.fetchActivities(
                project,
                accessToken,
                startedAt.minusDays(INITIAL_SYNC_DAYS),
                INITIAL_SYNC_MAX_PAGES
        );
        persist(activityNormalizer.normalize(project, null, seeds));
        OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        projectPortOut.markSyncReady(project.getId(), completedAt);
        webhookDeliveryPortOut.complete(delivery.id(), false, completedAt);
    }

    private void processGithubDelivery(WebhookDelivery delivery) {
        Project project = projectPortOut.getById(delivery.projectId()).orElseThrow();
        WebhookActivitySeedFactory.Result normalized = webhookActivitySeedFactory.create(
                project,
                delivery.eventName(),
                delivery.deliveryId(),
                delivery.sanitizedPayload(),
                delivery.receivedAt()
        );
        persist(activityNormalizer.normalize(project, delivery.id(), normalized.seeds()));
        OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        webhookDeliveryPortOut.complete(delivery.id(), normalized.ignored(), completedAt);
        projectPortOut.markWebhookHealthy(
                project.getId(), delivery.deliveryId(), delivery.receivedAt()
        );
    }

    private void processWebhookTest(WebhookDelivery test) {
        Project project = projectPortOut.getById(test.projectId()).orElseThrow();
        WebhookTestPayloadFactory.SignedPayload signed = webhookTestPayloadFactory.create(project);
        webhookPortIn.receive(
                signed.signature(), "ping", signed.deliveryId(), signed.body()
        );
        WebhookDeliveryPortOut.RegisterResult ping = webhookDeliveryPortOut.register(
                project.getId(), signed.deliveryId(), "ping", null,
                Map.of(
                        "repository", Map.of("id", project.getGithubRepoId()),
                        "installation", Map.of("id", project.getGithubInstallationId())
                ),
                OffsetDateTime.now(ZoneOffset.UTC)
                        .plusDays(project.getSettings().webhookPayloadRetentionDays())
        );
        OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        webhookDeliveryPortOut.complete(ping.delivery().id(), false, completedAt);
        webhookDeliveryPortOut.recordTestDelivery(test.id(), signed.deliveryId());
        webhookDeliveryPortOut.complete(test.id(), false, completedAt);
        projectPortOut.markWebhookHealthy(project.getId(), signed.deliveryId(), completedAt);
    }

    /**
     * Activity를 저장합니다.
     * <pre>
     * - Activity는 WebhookDelivery와 별도의 트랜잭션에서 저장
     * - WebhookDelivery 처리 중 예외가 발생하더라도, Activity는 영향을 받지 않음
     * </pre>
     */
    private void persist(List<Activity> activities) {
        activityPortOut.saveAllIfAbsent(activities);
    }

    private void handleFailure(WebhookDelivery delivery, RuntimeException exception) {
        boolean terminal = delivery.attempts() >= MAX_ATTEMPTS;
        String code = failureCode(delivery, exception);
        String message = safeMessage(delivery);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 재시도 지연 시간은 30초 * (2 ^ (시도 횟수 - 1))로 계산
        long delaySeconds = 30L * (1L << Math.max(0, delivery.attempts() - 1));

        // 최종 실패 시, nextRetryAt 무시
        webhookDeliveryPortOut.fail(
                delivery.id(), code, message, now.plusSeconds(delaySeconds), terminal
        );

        // 프로젝트 상태를 업데이트
        if (terminal && delivery.isInitialSync()) {
            projectPortOut.markSyncFailed(delivery.projectId(), code, message, now);
        } else if (!delivery.isWebhookTest() && !delivery.isInitialSync()) {
            projectPortOut.markWebhookDegraded(delivery.projectId(), code, message, now);
        }

        log.warn(
                "Collection work failed: workType={}, projectId={}, attempts={}, terminal={}, exceptionType={}",
                delivery.eventName(), delivery.projectId(), delivery.attempts(), terminal,
                exception.getClass().getSimpleName()
        );
    }

    private String failureCode(WebhookDelivery delivery, RuntimeException exception) {
        if (exception instanceof CustomException customException) {
            return customException.getErrorCode().getErrorCode();
        }
        return delivery.isInitialSync() ? "PROJECT_50001" : "WEBHOOK_50001";
    }

    private String safeMessage(WebhookDelivery delivery) {
        return delivery.isInitialSync()
                ? "프로젝트 최초 동기화에 실패했습니다."
                : "Webhook 처리에 실패했습니다.";
    }
}
