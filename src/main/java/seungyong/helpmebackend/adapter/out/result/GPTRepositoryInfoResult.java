package seungyong.helpmebackend.adapter.out.result;

public record GPTRepositoryInfoResult(
        String[] techStack,
        String projectSize,
        String[] entryPoints,
        String[] importantFiles
) {
}
