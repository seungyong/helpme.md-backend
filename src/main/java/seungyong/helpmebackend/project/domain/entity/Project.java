package seungyong.helpmebackend.project.domain.entity;

import lombok.Builder;
import lombok.Getter;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.time.OffsetDateTime;

@Getter
public class Project {
    private final Long id;
    private final Long userId;
    private final String repoFullName;
    private final Long githubRepoId;
    private final Long githubInstallationId;
    private final String defaultBranch;
    private final boolean privateRepository;
    private final ProjectStatus status;
    private final ProjectSync sync;
    private final ProjectWebhook webhook;
    private ProjectSettings settings;
    private final ProjectDeletion deletion;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Project(Long id, Long userId, String repoFullName) {
        this(
                id,
                userId,
                repoFullName,
                null,
                null,
                null,
                false,
                ProjectStatus.ACTIVE,
                ProjectSync.pending(),
                ProjectWebhook.waiting(),
                ProjectSettings.defaults(),
                ProjectDeletion.none(),
                null,
                null
        );
    }

    @Builder
    public Project(
            Long id,
            Long userId,
            String repoFullName,
            Long githubRepoId,
            Long githubInstallationId,
            String defaultBranch,
            boolean privateRepository,
            ProjectStatus status,
            ProjectSync sync,
            ProjectWebhook webhook,
            ProjectSettings settings,
            ProjectDeletion deletion,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.repoFullName = repoFullName;
        this.githubRepoId = githubRepoId;
        this.githubInstallationId = githubInstallationId;
        this.defaultBranch = defaultBranch;
        this.privateRepository = privateRepository;
        this.status = status == null ? ProjectStatus.ACTIVE : status;
        this.sync = sync == null ? ProjectSync.pending() : sync;
        this.webhook = webhook == null ? ProjectWebhook.waiting() : webhook;
        this.settings = settings == null ? ProjectSettings.defaults() : settings;
        this.deletion = deletion == null ? ProjectDeletion.none() : deletion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isOwnedBy(Long targetUserId) {
        return userId != null && userId.equals(targetUserId);
    }

    public boolean isActive() {
        return status == ProjectStatus.ACTIVE;
    }

    public void changeSettings(ProjectSettings changedSettings) {
        this.settings = changedSettings;
    }

    public void recordUpdatedAt(OffsetDateTime changedAt) {
        this.updatedAt = changedAt;
    }
}
