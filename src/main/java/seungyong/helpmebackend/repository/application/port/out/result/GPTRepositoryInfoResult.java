package seungyong.helpmebackend.repository.application.port.out.result;

public record GPTRepositoryInfoResult(
        String[] techStack,
        String projectSize,
        String[] entryPoints,
        String[] importantFiles
) {
}
