package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.adapter.out.persistence.mapper.ProjectPersistenceMapper;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;

import java.util.Collection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

@Repository
@RequiredArgsConstructor
public class ProjectAdapter implements ProjectPortOut {
    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public Project save(Project project) {
        try {
            ProjectJpaEntity savedEntity = projectJpaRepository.saveAndFlush(
                    ProjectPersistenceMapper.INSTANCE.toJpaEntity(project)
            );
            return ProjectPersistenceMapper.INSTANCE.toDomainEntity(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            String details = rootMessage(exception).toLowerCase(java.util.Locale.ROOT);
            if (details.contains("project_limit") || details.contains("project limit")) {
                throw new CustomException(ProjectErrorCode.PROJECT_LIMIT_EXCEEDED);
            }
            if (details.contains("uq_projects_user_github_repo")
                    || details.contains("unique_projects_user_repo")) {
                throw new CustomException(ProjectErrorCode.PROJECT_ALREADY_CONNECTED);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public Project updateSettings(Long projectId, ProjectSettings settings) {
        ProjectJpaEntity entity = projectJpaRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        entity.updateSettings(settings);
        ProjectJpaEntity savedEntity = projectJpaRepository.saveAndFlush(entity);
        return ProjectPersistenceMapper.INSTANCE.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Project> getById(Long projectId) {
        return projectJpaRepository.findById(projectId)
                .map(ProjectPersistenceMapper.INSTANCE::toDomainEntity);
    }

    @Override
    public Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName) {
        Optional<ProjectJpaEntity> entityOptional = projectJpaRepository.findByUser_IdAndRepoFullName(userId, repoFullName);
        return entityOptional.map(ProjectPersistenceMapper.INSTANCE::toDomainEntity);
    }

    @Override
    public Optional<Project> getByUserIdAndGithubRepoId(Long userId, Long githubRepoId) {
        return projectJpaRepository.findByUser_IdAndGithubRepoId(userId, githubRepoId)
                .map(ProjectPersistenceMapper.INSTANCE::toDomainEntity);
    }

    @Override
    public long countByUserId(Long userId) {
        return projectJpaRepository.countByUser_Id(userId);
    }

    @Override
    public List<Project> getActiveByGithubRepository(Long installationId, Long githubRepoId) {
        return projectJpaRepository.findAllByGithubInstallationIdAndGithubRepoIdAndStatus(
                        installationId, githubRepoId, ProjectStatus.ACTIVE
                ).stream()
                .map(ProjectPersistenceMapper.INSTANCE::toDomainEntity)
                .toList();
    }

    @Override
    @Transactional
    public Project markSyncPending(Long projectId) {
        ProjectJpaEntity entity = getEntity(projectId);
        entity.markSyncPending();
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Project markSyncRunning(Long projectId, OffsetDateTime startedAt) {
        ProjectJpaEntity entity = getEntity(projectId);
        entity.markSyncRunning(startedAt);
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Project markSyncReady(Long projectId, OffsetDateTime completedAt) {
        ProjectJpaEntity entity = getEntity(projectId);
        entity.markSyncReady(completedAt);
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Project markSyncFailed(
            Long projectId, String code, String message, OffsetDateTime failedAt
    ) {
        ProjectJpaEntity entity = getEntity(projectId);
        entity.markSyncFailed(code, message, failedAt);
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Project markWebhookHealthy(
            Long projectId, String deliveryId, OffsetDateTime receivedAt
    ) {
        ProjectJpaEntity entity = getEntity(projectId);
        entity.markWebhookHealthy(deliveryId, receivedAt);
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Project markWebhookDegraded(
            Long projectId, String code, String message, OffsetDateTime checkedAt
    ) {
        ProjectJpaEntity entity = getEntity(projectId);
        entity.markWebhookDegraded(code, message, checkedAt);
        return toDomain(entity);
    }

    private ProjectJpaEntity getEntity(Long projectId) {
        return projectJpaRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private Project toDomain(ProjectJpaEntity entity) {
        projectJpaRepository.saveAndFlush(entity);
        return ProjectPersistenceMapper.INSTANCE.toDomainEntity(entity);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }

    @Override
    public Set<Long> getConnectedGithubRepoIds(Long userId, Collection<Long> githubRepoIds) {
        if (githubRepoIds.isEmpty()) {
            return Set.of();
        }

        return projectJpaRepository.findAllByUser_IdAndGithubRepoIdIn(userId, githubRepoIds)
                .stream()
                .map(ProjectJpaEntity::getGithubRepoId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
