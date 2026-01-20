package seungyong.helpmebackend.infrastructure.gpt.dto;

import java.util.List;

public record EvaluationContent(
        float rating,
        List<String> contents
) {
}
