package seungyong.helpmebackend.usecase.port.out.evaluation;

import seungyong.helpmebackend.domain.entity.evaluation.Evaluation;

public interface EvaluationPortOut {
    Evaluation save(Evaluation evaluation);

    Evaluation getByFullName(String fullName);
}
