package seungyong.helpmebackend.adapter.out.gpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GPTDraftReadmeGenerationSchema(
        @JsonProperty(value = "content", required = true)
        String content
) {
}
