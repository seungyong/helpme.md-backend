package seungyong.helpmebackend.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.adapter.out.persistence.entity.EvaluationJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.mapper.EvaluationPortOutMapper;
import seungyong.helpmebackend.adapter.out.persistence.repository.EvaluationJpaRepository;
import seungyong.helpmebackend.domain.entity.evaluation.Evaluation;
import seungyong.helpmebackend.usecase.port.out.evaluation.EvaluationPortOut;

@Repository
@RequiredArgsConstructor
public class EvaluationAdapter implements EvaluationPortOut {
    private final EvaluationJpaRepository evaluationJpaRepository;

    @Override
    public Evaluation save(Evaluation evaluation) {
        EvaluationJpaEntity entity = EvaluationPortOutMapper.INSTANCE.toEntity(evaluation);
        EvaluationJpaEntity savedEntity = evaluationJpaRepository.save(entity);
        return EvaluationPortOutMapper.INSTANCE.toDomain(savedEntity);
    }

    @Override
    public Evaluation getByFullName(String fullName) {
        EvaluationJpaEntity evaluation = evaluationJpaRepository.findByRepoFullName(fullName);

        if (evaluation == null) {
            return null;
        }

        return EvaluationPortOutMapper.INSTANCE.toDomain(evaluation);
    }
}
