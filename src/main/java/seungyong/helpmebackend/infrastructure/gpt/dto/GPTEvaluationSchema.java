package seungyong.helpmebackend.infrastructure.gpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GPTEvaluationSchema(
        @JsonProperty(value = "rating", required = true)
        float rating,
        @JsonProperty(value = "contents", required = true)
        String[] contents
) {
}
