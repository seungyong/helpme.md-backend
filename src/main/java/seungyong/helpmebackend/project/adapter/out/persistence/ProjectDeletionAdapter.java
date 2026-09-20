package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import seungyong.helpmebackend.global.application.deletion.DeletionRetryPolicy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.adapter.out.persistence.mapper.ProjectPersistenceMapper;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectDeletionAdapter implements ProjectDeletionPortOut {
    private static final List<ProjectStatus> DELETION_STATUSES = List.of(
            ProjectStatus.DELETING, ProjectStatus.DELETE_FAILED
    );

    private final ProjectJpaRepository repository;

    @Override
    @Transactional
    public Project requestDeletion(Long projectId, OffsetDateTime requestedAt) {
        ProjectJpaEntity entity = repository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (entity.getDeletionRequestedAt() != null) {
            return ProjectPersistenceMapper.INSTANCE.toDomainEntity(entity);
        }
        entity.requestDeletion(requestedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        return ProjectPersistenceMapper.INSTANCE.toDomainEntity(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void requestAllByUserId(Long userId, OffsetDateTime requestedAt) {
        repository.findAllByUser_IdAndStatusIn(userId, List.of(ProjectStatus.ACTIVE))
                .forEach(project -> project.requestDeletion(requestedAt));
        repository.flush();
    }

    @Override
    @Transactional
    public Optional<Project> claimNext(
            OffsetDateTime now,
            OffsetDateTime deletingBefore,
            OffsetDateTime retryBefore
    ) {
        List<ProjectJpaEntity> candidates = repository.findDeletionCandidates(
                ProjectStatus.DELETING,
                ProjectStatus.DELETE_FAILED,
                deletingBefore,
                retryBefore, now,
                PageRequest.of(0, 1)
        );
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        ProjectJpaEntity candidate = candidates.get(0);
        int claimed = repository.claimDeletion(
                candidate.getId(), candidate.getUpdatedAt(), now, DELETION_STATUSES
        );
        if (claimed == 0) {
            return Optional.empty();
        }
        return repository.findById(candidate.getId())
                .map(ProjectPersistenceMapper.INSTANCE::toDomainEntity);
    }

    @Override
    @Transactional
    public void markFailed(Long projectId, OffsetDateTime claimedAt, String errorCode, String errorMessage) {
        repository.findByIdForUpdate(projectId).filter(entity -> ownsClaim(entity, claimedAt)).ifPresent(entity -> {
            entity.markDeletionFailed(errorCode, errorMessage);
            entity.recordDeletionRetry(OffsetDateTime.now(java.time.ZoneOffset.UTC));
            if (entity.getDeletionAttempts() >= DeletionRetryPolicy.FAST_ATTEMPTS) {
                log.error("Deletion fast retries exhausted; daily retry scheduled: projectId={}", projectId);
            }
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockClaim(Long projectId, OffsetDateTime claimedAt) {
        return repository.findByIdForUpdate(projectId)
                .filter(entity -> ownsClaim(entity, claimedAt)).isPresent();
    }

    private boolean ownsClaim(ProjectJpaEntity entity, OffsetDateTime claimedAt) {
        return claimedAt != null && DELETION_STATUSES.contains(entity.getStatus())
                && entity.getUpdatedAt().isEqual(claimedAt);
    }

    @Override
    @Transactional
    public void hardDelete(Long projectId) {
        repository.findById(projectId).ifPresent(repository::delete);
        repository.flush();
    }
}
