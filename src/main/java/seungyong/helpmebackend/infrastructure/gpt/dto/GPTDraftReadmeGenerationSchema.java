package seungyong.helpmebackend.infrastructure.gpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GPTDraftReadmeGenerationSchema(
        @JsonProperty(value = "content", required = true)
        String content
) {
}
