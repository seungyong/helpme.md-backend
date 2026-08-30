package seungyong.helpmebackend.project.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, Long> {
    Optional<ProjectJpaEntity> findByUser_IdAndRepoFullName(Long userId, String repoFullName);

    Optional<ProjectJpaEntity> findByUser_IdAndGithubRepoId(Long userId, Long githubRepoId);

    long countByUser_Id(Long userId);

    List<ProjectJpaEntity> findAllByGithubInstallationIdAndGithubRepoIdAndStatus(
            Long githubInstallationId,
            Long githubRepoId,
            ProjectStatus status
    );

    List<ProjectJpaEntity> findAllByStatus(ProjectStatus status);

    List<ProjectJpaEntity> findAllByUser_IdAndGithubRepoIdIn(Long userId, Collection<Long> githubRepoIds);
}
