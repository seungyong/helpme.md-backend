package seungyong.helpmebackend.repository.adapter.out.gpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GPTRepositoryAnalyzeSchema(
        @JsonProperty(value = "techStack", required = true)
        String[] techStack,
        @JsonProperty(value = "entryPoints", required = true)
        String[] entryPoints,
        @JsonProperty(value = "projectSize", required = true)
        ProjectSize projectSize,
        @JsonProperty(value = "importantFiles", required = true)
        String[] importantFiles
) {
    public enum ProjectSize {
        small,
        medium,
        large
    }
}
