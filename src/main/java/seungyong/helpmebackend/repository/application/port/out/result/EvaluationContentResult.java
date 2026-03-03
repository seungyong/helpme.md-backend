package seungyong.helpmebackend.repository.application.port.out.result;

import java.util.List;

public record EvaluationContentResult(
        float rating,
        List<String> contents
) {
}
