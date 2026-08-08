package seungyong.helpmebackend.project.adapter.out.persistence.mapper;

import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectDeletion;
import seungyong.helpmebackend.project.domain.entity.ProjectOperationError;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.entity.ProjectSync;
import seungyong.helpmebackend.project.domain.entity.ProjectWebhook;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;

import java.util.Arrays;

public final class ProjectPersistenceMapper {
    public static final ProjectPersistenceMapper INSTANCE = new ProjectPersistenceMapper();

    private ProjectPersistenceMapper() {
    }

    public ProjectJpaEntity toJpaEntity(Project project) {
        ProjectSettings settings = project.getSettings();
        ProjectSync sync = project.getSync();
        ProjectWebhook webhook = project.getWebhook();
        ProjectDeletion deletion = project.getDeletion();

        return ProjectJpaEntity.builder()
                .id(project.getId())
                .user(UserJpaEntity.builder().id(project.getUserId()).build())
                .repoFullName(project.getRepoFullName())
                .githubRepoId(project.getGithubRepoId())
                .githubInstallationId(project.getGithubInstallationId())
                .defaultBranch(project.getDefaultBranch())
                .trackedBranches(settings.trackedBranches().toArray(String[]::new))
                .trackAllBranches(settings.trackAllBranches())
                .privateRepository(project.isPrivateRepository())
                .status(project.getStatus())
                .syncStatus(sync.status())
                .syncStartedAt(sync.startedAt())
                .syncCompletedAt(sync.completedAt())
                .syncErrorCode(errorCode(sync.error()))
                .syncErrorMessage(errorMessage(sync.error()))
                .webhookStatus(webhook.status())
                .timezone(settings.timezone())
                .dailyEnabled(settings.daily().enabled())
                .dailyGenerationTime(settings.daily().generationTime())
                .weeklyEnabled(settings.weekly().enabled())
                .weeklyGenerationDay(settings.weekly().generationDay().getDatabaseValue())
                .weeklyGenerationTime(settings.weekly().generationTime())
                .webhookPayloadRetentionDays(settings.webhookPayloadRetentionDays())
                .webhookLastCheckedAt(webhook.lastCheckedAt())
                .webhookLastReceivedAt(webhook.lastReceivedAt())
                .webhookLastDeliveryId(webhook.lastDeliveryId())
                .webhookErrorCode(errorCode(webhook.error()))
                .webhookErrorMessage(errorMessage(webhook.error()))
                .deletionRequestedAt(deletion.requestedAt())
                .deletionErrorCode(errorCode(deletion.error()))
                .deletionErrorMessage(errorMessage(deletion.error()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public Project toDomainEntity(ProjectJpaEntity entity) {
        ProjectSettings settings = new ProjectSettings(
                Arrays.asList(entity.getTrackedBranches()),
                entity.isTrackAllBranches(),
                entity.getTimezone(),
                new ProjectSettings.DailyReflectionSchedule(
                        entity.isDailyEnabled(),
                        entity.getDailyGenerationTime()
                ),
                new ProjectSettings.WeeklyReflectionSchedule(
                        entity.isWeeklyEnabled(),
                        ReflectionWeekday.fromDatabaseValue(entity.getWeeklyGenerationDay()),
                        entity.getWeeklyGenerationTime()
                ),
                entity.getWebhookPayloadRetentionDays()
        );

        return Project.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .repoFullName(entity.getRepoFullName())
                .githubRepoId(entity.getGithubRepoId())
                .githubInstallationId(entity.getGithubInstallationId())
                .defaultBranch(entity.getDefaultBranch())
                .privateRepository(entity.isPrivateRepository())
                .status(entity.getStatus())
                .sync(new ProjectSync(
                        entity.getSyncStatus(),
                        entity.getSyncStartedAt(),
                        entity.getSyncCompletedAt(),
                        error(entity.getSyncErrorCode(), entity.getSyncErrorMessage())
                ))
                .webhook(new ProjectWebhook(
                        entity.getWebhookStatus(),
                        entity.getWebhookLastCheckedAt(),
                        entity.getWebhookLastReceivedAt(),
                        entity.getWebhookLastDeliveryId(),
                        error(entity.getWebhookErrorCode(), entity.getWebhookErrorMessage())
                ))
                .settings(settings)
                .deletion(new ProjectDeletion(
                        entity.getDeletionRequestedAt(),
                        error(entity.getDeletionErrorCode(), entity.getDeletionErrorMessage())
                ))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ProjectOperationError error(String code, String message) {
        return code == null && message == null ? null : new ProjectOperationError(code, message);
    }

    private String errorCode(ProjectOperationError error) {
        return error == null ? null : error.code();
    }

    private String errorMessage(ProjectOperationError error) {
        return error == null ? null : error.message();
    }
}
