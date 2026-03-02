package seungyong.helpmebackend.infrastructure.gpt.dto;

public record PromptContext(
        String repositoryInfo,
        String entryPoints,
        String importantFiles,
        String techStacks,
        String projectSize
) {}
