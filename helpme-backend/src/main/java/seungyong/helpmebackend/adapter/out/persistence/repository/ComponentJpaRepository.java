package seungyong.helpmebackend.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.adapter.out.persistence.entity.ComponentJpaEntity;

import java.util.List;

public interface ComponentJpaRepository extends JpaRepository<ComponentJpaEntity, Long> {
    List<ComponentJpaEntity> findByRepoFullName(String repoFullName);
}
