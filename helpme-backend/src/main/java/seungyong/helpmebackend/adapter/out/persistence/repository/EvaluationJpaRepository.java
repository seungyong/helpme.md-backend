package seungyong.helpmebackend.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.adapter.out.persistence.entity.EvaluationJpaEntity;

import java.util.Optional;

public interface EvaluationJpaRepository extends JpaRepository<EvaluationJpaEntity, Long> {
    Optional<EvaluationJpaEntity> findByRepoFullName(String fullName);
}
