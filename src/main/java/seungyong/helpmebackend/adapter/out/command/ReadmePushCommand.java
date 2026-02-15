package seungyong.helpmebackend.adapter.out.command;

public record ReadmePushCommand(
        RepoInfoCommand repoInfo,
        String branch,
        String newContent,
        String readmeSha,
        String commitMessage
) {
}
