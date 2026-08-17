package seungyong.helpmebackend.webhook.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.webhook.adapter.out.persistence.entity.WebhookDeliveryJpaEntity;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

interface WebhookDeliveryJpaRepository extends JpaRepository<WebhookDeliveryJpaEntity, Long> {
    Optional<WebhookDeliveryJpaEntity> findByProject_IdAndDeliveryId(Long projectId, String deliveryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select w from WebhookDelivery w
            where w.status = :received
               or (w.status = :failed and w.nextRetryAt is not null and w.nextRetryAt <= :now)
            order by w.receivedAt asc, w.id asc
            """)
    List<WebhookDeliveryJpaEntity> findClaimable(
            @Param("received") WebhookDeliveryStatus received,
            @Param("failed") WebhookDeliveryStatus failed,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    @Query("""
            select w from WebhookDelivery w
            where w.status = :processing and w.processingStartedAt < :stuckBefore
            """)
    List<WebhookDeliveryJpaEntity> findStuck(
            @Param("processing") WebhookDeliveryStatus processing,
            @Param("stuckBefore") OffsetDateTime stuckBefore
    );

    @Query("""
            select w from WebhookDelivery w
            where w.eventName = :eventName and w.project.id = :projectId
              and w.deliveryId = :deliveryId
            """)
    Optional<WebhookDeliveryJpaEntity> findInternalWork(
            @Param("eventName") String eventName,
            @Param("projectId") Long projectId,
            @Param("deliveryId") String deliveryId
    );

    List<WebhookDeliveryJpaEntity> findAllByEventNameAndStatusInAndPayloadExpiresAtBefore(
            String eventName,
            List<WebhookDeliveryStatus> statuses,
            OffsetDateTime now
    );

    List<WebhookDeliveryJpaEntity> findAllBySanitizedPayloadIsNotNullAndPayloadExpiresAtBefore(
            OffsetDateTime now
    );
}
