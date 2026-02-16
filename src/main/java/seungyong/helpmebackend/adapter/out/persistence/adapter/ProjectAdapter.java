package seungyong.helpmebackend.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.mapper.ProjectPortOutMapper;
import seungyong.helpmebackend.adapter.out.persistence.repository.ProjectJpaRepository;
import seungyong.helpmebackend.domain.entity.project.Project;
import seungyong.helpmebackend.usecase.port.out.project.ProjectPortOut;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectAdapter implements ProjectPortOut {
    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public Project save(Project project) {
        ProjectJpaEntity savedEntity = projectJpaRepository.save(ProjectPortOutMapper.INSTANCE.toEntity(project));
        return ProjectPortOutMapper.INSTANCE.toDomain(savedEntity);
    }

    @Override
    public Optional<Project> getByUserIdAndRepoFullName(Long userId, String repoFullName) {
        Optional<ProjectJpaEntity> entityOptional = projectJpaRepository.findByUser_IdAndRepoFullName(userId, repoFullName);
        return entityOptional.map(ProjectPortOutMapper.INSTANCE::toDomain);
    }
}
