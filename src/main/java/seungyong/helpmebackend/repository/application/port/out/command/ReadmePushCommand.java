package seungyong.helpmebackend.repository.application.port.out.command;

public record ReadmePushCommand(
        RepoInfoCommand repoInfo,
        String branch,
        String newContent,
        String readmeSha,
        String commitMessage
) {
}
