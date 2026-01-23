package seungyong.helpmebackend.adapter.out.command;

public record ReadmePushCommand(
        CommonRepoCommand repoInfo,
        String branch,
        String newContent,
        String readmeSha,
        String commitMessage
) {
}
