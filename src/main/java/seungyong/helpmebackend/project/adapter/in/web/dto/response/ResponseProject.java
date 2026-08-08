package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectOperationError;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;

import java.time.OffsetDateTime;
import java.util.List;

public record ResponseProject(
        Long id,
        String repoFullname,
        Long githubRepoId,
        String defaultBranch,
        List<String> trackedBranches,
        boolean trackAllBranches,
        @JsonProperty("isPrivate") boolean privateRepository,
        String status,
        Sync sync,
        Webhook webhook,
        String timezone
) {
    public static ResponseProject from(Project project) {
        return new ResponseProject(
                project.getId(),
                project.getRepoFullName(),
                project.getGithubRepoId(),
                project.getDefaultBranch(),
                project.getSettings().trackedBranches(),
                project.getSettings().trackAllBranches(),
                project.isPrivateRepository(),
                project.getStatus().getDatabaseValue(),
                Sync.from(project.getSync()),
                Webhook.from(project.getWebhook()),
                project.getSettings().timezone()
        );
    }

    public record Sync(
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            OperationError error
    ) {
        public static Sync from(ProjectSync sync) {
            return new Sync(
                    sync.status().getDatabaseValue(),
                    sync.startedAt(),
                    sync.completedAt(),
                    OperationError.from(sync.error())
            );
        }
    }

    public record Webhook(
            String status,
            OffsetDateTime lastCheckedAt,
            OffsetDateTime lastReceivedAt,
            String lastDeliveryId,
            OperationError error
    ) {
        public static Webhook from(ProjectWebhook webhook) {
            return new Webhook(
                    webhook.status().getDatabaseValue(),
                    webhook.lastCheckedAt(),
                    webhook.lastReceivedAt(),
                    webhook.lastDeliveryId(),
                    OperationError.from(webhook.error())
            );
        }
    }

    public record OperationError(
            String code,
            String message,
            boolean retryable
    ) {
        public static OperationError from(ProjectOperationError error) {
            return error == null ? null : new OperationError(error.code(), error.message(), true);
        }
    }
}
