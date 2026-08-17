package seungyong.helpmebackend.webhook.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.webhook.adapter.out.persistence.entity.WebhookDeliveryJpaEntity;
import seungyong.helpmebackend.webhook.application.port.out.WebhookDeliveryPortOut;
import seungyong.helpmebackend.webhook.application.port.out.WebhookWorkPortOut;
import seungyong.helpmebackend.webhook.domain.entity.WebhookDelivery;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WebhookDeliveryAdapter implements WebhookDeliveryPortOut, WebhookWorkPortOut {
    private static final String INITIAL_SYNC_KEY = "_initial_sync";
    private static final String TEST_KEY_PREFIX = "_test:";
    private static final int MAX_ATTEMPTS = 3;

    private final WebhookDeliveryJpaRepository webhookDeliveryJpaRepository;
    private final ProjectPortOut projectPortOut;
    private final ObjectMapper objectMapper;

    @Override
    public RegisterResult register(
            Long projectId,
            String deliveryId,
            String eventName,
            String action,
            Map<String, Object> sanitizedPayload,
            OffsetDateTime payloadExpiresAt
    ) {
        Optional<WebhookDeliveryJpaEntity> existing = webhookDeliveryJpaRepository
                .findByProject_IdAndDeliveryId(projectId, deliveryId);
        if (existing.isPresent()) {
            return new RegisterResult(toDomain(existing.get()), false);
        }
        WebhookDeliveryJpaEntity entity = WebhookDeliveryJpaEntity.builder()
                .project(ProjectJpaEntity.builder().id(projectId).build())
                .deliveryId(deliveryId)
                .eventName(eventName)
                .action(action)
                .sanitizedPayload(toJson(sanitizedPayload))
                .payloadExpiresAt(payloadExpiresAt)
                .build();
        try {
            return new RegisterResult(
                    toDomain(webhookDeliveryJpaRepository.saveAndFlush(entity)), true
            );
        } catch (DataIntegrityViolationException exception) {
            // 동시에 동일한 deliveryId로 등록 시도 시, 이미 존재하는 경우를 처리
            WebhookDeliveryJpaEntity duplicate = webhookDeliveryJpaRepository
                    .findByProject_IdAndDeliveryId(projectId, deliveryId)
                    .orElseThrow(() -> exception);
            return new RegisterResult(toDomain(duplicate), false);
        }
    }

    @Override
    @Transactional
    public Optional<WebhookDelivery> claimNext(
            OffsetDateTime now,
            OffsetDateTime stuckBefore
    ) {


        // 중단된 작업을 복구하고, 재시도 횟수가 최대치를 초과한 경우에는 실패 상태로 마킹
        // 재시도 시간이 null인 경우는 더 이상 재시도하지 않도록 함
        for (WebhookDeliveryJpaEntity stuck : webhookDeliveryJpaRepository.findStuck(
                WebhookDeliveryStatus.PROCESSING, stuckBefore)) {

            // MAX_ATTEMPTS 이상이면 terminal 상태로 간주하고, 그렇지 않으면 재시도 가능 상태로 간주
            boolean terminal = stuck.getAttempts() >= MAX_ATTEMPTS;
            OffsetDateTime retryAt = terminal ? null : now;

            String code = errorCode(stuck);
            String message = "중단된 작업을 복구했습니다.";
            // 중단된 작업을 실패 상태로 마킹
            // 최종 실패 상태인 경우에는 재시도 시간을 null로 설정하여 더 이상 재시도하지 않도록 함
            stuck.fail(
                    code, message, retryAt
            );

            // 프로젝트 상태를 최종 실패로 마킹
            if (terminal && WebhookDelivery.INITIAL_SYNC_EVENT.equals(stuck.getEventName())) {
                // 중단된 작업이 초기 동기화 이벤트인 경우, 프로젝트 동기화 상태를 실패로 마킹
                projectPortOut.markSyncFailed(stuck.getProject().getId(), code, message, now);
            } else if (terminal && !WebhookDelivery.WEBHOOK_TEST_EVENT.equals(stuck.getEventName())) {
                // 중단된 작업이 테스트 이벤트가 아닌 경우, 프로젝트 웹훅 상태를 저하로 마킹
                projectPortOut.markWebhookDegraded(
                        stuck.getProject().getId(), code, message, now
                );
            }
        }

        // 재시도 가능한 작업 중에서 가장 오래된 작업을 조회
        // @Lock(LockModeType.PESSIMISTIC_WRITE)를 사용하여 다른 트랜잭션에서 동일한 작업을 claim하지 못하도록 함
        List<WebhookDeliveryJpaEntity> claimable = webhookDeliveryJpaRepository.findClaimable(
                WebhookDeliveryStatus.RECEIVED,
                WebhookDeliveryStatus.FAILED,
                now,
                PageRequest.of(0, 1)
        );
        if (claimable.isEmpty()) {
            return Optional.empty();
        }

        // findClaimable의 PESSIMISTIC_WRITE Lock으로 작업 행을 선점 (SELECT FOR UPDATE)
        WebhookDeliveryJpaEntity entity = claimable.get(0);

        // PROCESSING 상태로 변경하고, 처리 시작 시간을 기록 (재시도 가능하게 PROCESSING 상태로 변경)
        entity.claim(now);

        // PROCESSING 상태 변경을 즉시 DB에 전달, 행 Lock은 현재 transaction이 끝날 때까지 유지
        webhookDeliveryJpaRepository.flush();
        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public void complete(Long id, boolean ignored, OffsetDateTime processedAt) {
        getEntity(id).complete(ignored, processedAt);
    }

    @Override
    @Transactional
    public void fail(
            Long id,
            String errorCode,
            String errorMessage,
            OffsetDateTime nextRetryAt,
            boolean terminal
    ) {
        getEntity(id).fail(errorCode, errorMessage, terminal ? null : nextRetryAt);
    }

    @Override
    @Transactional
    public void recordTestDelivery(Long id, String deliveryId) {
        getEntity(id).recordRelatedDelivery(deliveryId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WebhookDelivery> getTest(Long projectId, String testId) {
        return webhookDeliveryJpaRepository.findInternalWork(
                        WebhookDelivery.WEBHOOK_TEST_EVENT,
                        projectId,
                        TEST_KEY_PREFIX + testId
                ).map(this::toDomain);
    }

    @Override
    @Transactional
    public int expireTimedOutTests(OffsetDateTime now, String errorCode, String errorMessage) {
        List<WebhookDeliveryJpaEntity> timedOut = webhookDeliveryJpaRepository
                .findAllByEventNameAndStatusInAndPayloadExpiresAtBefore(
                        WebhookDelivery.WEBHOOK_TEST_EVENT,
                        List.of(WebhookDeliveryStatus.RECEIVED, WebhookDeliveryStatus.PROCESSING),
                        now
                );
        timedOut.forEach(entity -> entity.fail(errorCode, errorMessage, null));
        return timedOut.size();
    }

    @Override
    @Transactional
    public int purgeExpiredPayloads(OffsetDateTime now) {
        List<WebhookDeliveryJpaEntity> expired = webhookDeliveryJpaRepository
                .findAllBySanitizedPayloadIsNotNullAndPayloadExpiresAtBefore(now);
        expired.forEach(entity -> entity.purgePayload(now));
        return expired.size();
    }

    @Override
    public void registerInitialSync(Long projectId) {
        register(
                projectId,
                INITIAL_SYNC_KEY,
                WebhookDelivery.INITIAL_SYNC_EVENT,
                null,
                Map.of(),
                null
        );
    }

    @Override
    @Transactional
    public void retryInitialSync(Long projectId) {
        WebhookDeliveryJpaEntity entity = webhookDeliveryJpaRepository.findInternalWork(
                WebhookDelivery.INITIAL_SYNC_EVENT, projectId, INITIAL_SYNC_KEY
        ).orElseGet(() -> webhookDeliveryJpaRepository.save(
                WebhookDeliveryJpaEntity.builder()
                        .project(ProjectJpaEntity.builder().id(projectId).build())
                        .deliveryId(INITIAL_SYNC_KEY)
                        .eventName(WebhookDelivery.INITIAL_SYNC_EVENT)
                        .build()
        ));
        entity.reset();
        projectPortOut.markSyncPending(projectId);
    }

    @Override
    public RegisterResult registerTest(Long projectId, String testId, OffsetDateTime deadline) {
        return register(
                projectId,
                TEST_KEY_PREFIX + testId,
                WebhookDelivery.WEBHOOK_TEST_EVENT,
                null,
                Map.of(),
                deadline
        );
    }

    private WebhookDeliveryJpaEntity getEntity(Long id) {
        return webhookDeliveryJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Webhook work disappeared"));
    }

    private WebhookDelivery toDomain(WebhookDeliveryJpaEntity entity) {
        Map<String, Object> payload = entity.getSanitizedPayload() == null
                ? Map.of()
                : objectMapper.convertValue(entity.getSanitizedPayload(), new TypeReference<>() { });
        return new WebhookDelivery(
                entity.getId(), entity.getProject().getId(), entity.getDeliveryId(),
                entity.getEventName(), entity.getAction(), entity.getStatus(), payload,
                entity.getPayloadExpiresAt(), entity.getPayloadPurgedAt(), entity.getAttempts(),
                entity.getNextRetryAt(), entity.getProcessingStartedAt(), entity.getProcessedAt(),
                entity.getErrorCode(), entity.getErrorMessage(), entity.getReceivedAt(),
                entity.getUpdatedAt()
        );
    }

    private JsonNode toJson(Map<String, Object> payload) {
        return payload == null || payload.isEmpty() ? null : objectMapper.valueToTree(payload);
    }

    private String errorCode(WebhookDeliveryJpaEntity entity) {
        return WebhookDelivery.INITIAL_SYNC_EVENT.equals(entity.getEventName())
                ? "PROJECT_50001" : "WEBHOOK_50001";
    }
}
