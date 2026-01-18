package seungyong.helpmebackend.usecase.port.out.evaluation;

import seungyong.helpmebackend.domain.entity.evaluation.Evaluation;

import java.util.Optional;

public interface EvaluationPortOut {
    Evaluation save(Evaluation evaluation);

    Optional<Evaluation> getByFullName(String fullName);
}
