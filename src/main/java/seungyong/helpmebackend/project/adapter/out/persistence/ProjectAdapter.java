package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProjectAdapter implements ProjectPortOut {
    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public Project save(Project project) {
        ProjectJpaEntity savedEntity = projectJpaRepository.save(
                ProjectPersistenceMapper.INSTANCE.toJpaEntity(project)
        );
        return ProjectPersistenceMapper.INSTANCE.toDomainEntity(savedEntity);
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
