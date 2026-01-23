package seungyong.helpmebackend.adapter.out.command;

public record CreatePullRequestCommand(
        CommonRepoCommand repoInfo,
        String head,
        String base,
        String title,
        String body
) {
}
