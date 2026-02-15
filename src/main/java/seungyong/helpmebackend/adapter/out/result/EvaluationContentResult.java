package seungyong.helpmebackend.adapter.out.result;

import java.util.List;

public record EvaluationContentResult(
        float rating,
        List<String> contents
) {
}
