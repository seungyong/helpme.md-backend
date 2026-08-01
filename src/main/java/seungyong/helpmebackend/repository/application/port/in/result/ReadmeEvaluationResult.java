package seungyong.helpmebackend.repository.application.port.in.result;

import java.util.List;

public record ReadmeEvaluationResult(
        Float rating,
        List<String> contents
) {
}
