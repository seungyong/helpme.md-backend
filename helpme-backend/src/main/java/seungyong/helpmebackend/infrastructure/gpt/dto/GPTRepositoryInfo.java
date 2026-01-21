package seungyong.helpmebackend.infrastructure.gpt.dto;

public record GPTRepositoryInfo(
        String[] techStack,
        String projectSize,
        String[] entryPoints
) {
}
