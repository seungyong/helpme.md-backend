package seungyong.helpmebackend.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.adapter.out.persistence.entity.EvaluationJpaEntity;

public interface EvaluationJpaRepository extends JpaRepository<EvaluationJpaEntity, Long> {
    EvaluationJpaEntity findByRepoFullName(String fullName);
}
