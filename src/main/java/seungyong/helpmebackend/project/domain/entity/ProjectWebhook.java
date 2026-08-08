package seungyong.helpmebackend.project.domain.entity;

import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;

import java.time.OffsetDateTime;

public record ProjectWebhook(
        ProjectWebhookStatus status,
        OffsetDateTime lastCheckedAt,
        OffsetDateTime lastReceivedAt,
        String lastDeliveryId,
        ProjectOperationError error
) {
    public ProjectWebhook {
        status = status == null ? ProjectWebhookStatus.WAITING : status;
    }

    public static ProjectWebhook waiting() {
        return new ProjectWebhook(ProjectWebhookStatus.WAITING, null, null, null, null);
    }
}
