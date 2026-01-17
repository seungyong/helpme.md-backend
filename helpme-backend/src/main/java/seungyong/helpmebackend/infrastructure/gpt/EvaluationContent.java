package seungyong.helpmebackend.infrastructure.gpt;

import java.util.List;

public record EvaluationContent(
        float rating,
        List<String> contents
) {
}
