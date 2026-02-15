package seungyong.helpmebackend.infrastructure.gpt.dto;

public record PromptContext(
        String languages,
        String latestCommits,
        String middleCommits,
        String oldCommits,
        String tree,
        String entryPoints,
        String importantFiles,
        String techStacks,
        String projectSize
) {}
