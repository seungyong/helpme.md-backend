package seungyong.helpmebackend.webhook.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.webhook.adapter.out.persistence.entity.WebhookDeliveryJpaEntity;
import seungyong.helpmebackend.webhook.domain.entity.WebhookDelivery;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryAdapterTest {
    @Mock private WebhookDeliveryJpaRepository repository;
    @Mock private ProjectPortOut projectPortOut;
    private WebhookDeliveryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WebhookDeliveryAdapter(repository, projectPortOut, new ObjectMapper());
    }

    @Test
    @DisplayName("재시도 한도에 도달한 중단 최초 sync를 실패 상태로 복구")
    void claimNext_stuckInitialSync_terminalFailure() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-17T10:00:00Z");
        WebhookDeliveryJpaEntity stuck = delivery((short) 3);
        given(repository.findStuck(
                WebhookDeliveryStatus.PROCESSING, now.minusMinutes(5)
        )).willReturn(List.of(stuck));
        given(repository.findClaimable(
                eq(WebhookDeliveryStatus.RECEIVED),
                eq(WebhookDeliveryStatus.FAILED),
                eq(now),
                any()
        )).willReturn(List.of());

        assertThat(adapter.claimNext(now, now.minusMinutes(5))).isEmpty();

        assertThat(stuck.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(stuck.getNextRetryAt()).isNull();
        verify(projectPortOut).markSyncFailed(
                eq(10L), eq("PROJECT_50001"), any(), eq(now)
        );
    }

    @Test
    @DisplayName("재시도 가능한 중단 작업은 즉시 다시 claim")
    void claimNext_stuckInitialSync_retry() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-17T10:00:00Z");
        WebhookDeliveryJpaEntity stuck = delivery((short) 2);
        given(repository.findStuck(
                WebhookDeliveryStatus.PROCESSING, now.minusMinutes(5)
        )).willReturn(List.of(stuck));
        given(repository.findClaimable(
                eq(WebhookDeliveryStatus.RECEIVED),
                eq(WebhookDeliveryStatus.FAILED),
                eq(now),
                any()
        )).willReturn(List.of(stuck));

        var claimed = adapter.claimNext(now, now.minusMinutes(5));

        assertThat(claimed).isPresent();
        assertThat(claimed.orElseThrow().status())
                .isEqualTo(WebhookDeliveryStatus.PROCESSING);
        assertThat(claimed.orElseThrow().attempts()).isEqualTo((short) 3);
        verify(projectPortOut, never()).markSyncFailed(any(), any(), any(), any());
        verify(repository).flush();
    }

    private WebhookDeliveryJpaEntity delivery(short attempts) {
        return WebhookDeliveryJpaEntity.builder()
                .id(100L)
                .project(ProjectJpaEntity.builder().id(10L).build())
                .deliveryId("_initial_sync")
                .eventName(WebhookDelivery.INITIAL_SYNC_EVENT)
                .status(WebhookDeliveryStatus.PROCESSING)
                .attempts(attempts)
                .processingStartedAt(OffsetDateTime.parse("2026-08-17T09:00:00Z"))
                .receivedAt(OffsetDateTime.parse("2026-08-17T09:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-08-17T09:00:00Z"))
                .build();
    }
}
