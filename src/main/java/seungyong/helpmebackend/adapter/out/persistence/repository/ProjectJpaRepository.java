package seungyong.helpmebackend.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.adapter.out.persistence.entity.ProjectJpaEntity;

import java.util.Optional;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, Long> {
    Optional<ProjectJpaEntity> findByUser_IdAndRepoFullName(Long userId, String repoFullName);
}
