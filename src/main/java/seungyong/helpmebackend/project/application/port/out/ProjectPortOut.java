package seungyong.helpmebackend.project.application.port.out;

import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;

import java.util.Collection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectPortOut {
    Project save(Project project);

    Project updateSettings(Long projectId, ProjectSettings settings);

    Optional<Project> getById(Long projectId);

    Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName);

    Optional<Project> getByUserIdAndGithubRepoId(Long userId, Long githubRepoId);

    long countByUserId(Long userId);

    List<Project> getActiveByGithubRepository(Long installationId, Long githubRepoId);

    Project markSyncPending(Long projectId);

    Project markSyncRunning(Long projectId, OffsetDateTime startedAt);

    Project markSyncReady(Long projectId, OffsetDateTime completedAt);

    Project markSyncFailed(Long projectId, String code, String message, OffsetDateTime failedAt);

    Project markWebhookHealthy(Long projectId, String deliveryId, OffsetDateTime receivedAt);

    Project markWebhookDegraded(Long projectId, String code, String message, OffsetDateTime checkedAt);

    Set<Long> getConnectedGithubRepoIds(Long userId, Collection<Long> githubRepoIds);
}
