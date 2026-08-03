package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOutMapper;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProjectAdapter implements ProjectPortOut {
    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public Project save(Project project) {
        ProjectJpaEntity savedEntity = projectJpaRepository.save(ProjectPortOutMapper.INSTANCE.toJpaEntity(project));
        return ProjectPortOutMapper.INSTANCE.toDomain(savedEntity);
    }

    @Override
    public Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName) {
        Optional<ProjectJpaEntity> entityOptional = projectJpaRepository.findByUser_IdAndRepoFullName(userId, repoFullName);
        return entityOptional.map(ProjectPortOutMapper.INSTANCE::toDomain);
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
