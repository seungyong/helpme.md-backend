package seungyong.helpmebackend.domain.vo;

import lombok.Getter;

@Getter
public enum EvaluationStatus {
    GOOD,
    CREATED,
    IMPROVEMENT,
    NONE,
    ;

    public static EvaluationStatus getStatus(Float rating) {
        return rating >= 4.0 ? GOOD : IMPROVEMENT;
    }
}
