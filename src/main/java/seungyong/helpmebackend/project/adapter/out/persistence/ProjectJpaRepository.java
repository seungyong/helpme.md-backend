package seungyong.helpmebackend.project.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, Long> {
    Optional<ProjectJpaEntity> findByUser_IdAndRepoFullName(Long userId, String repoFullName);

    List<ProjectJpaEntity> findAllByUser_IdAndGithubRepoIdIn(Long userId, Collection<Long> githubRepoIds);
}
