package seungyong.helpmebackend.adapter.out.command;

public record CreatePullRequestCommand(
        RepoInfoCommand repoInfo,
        String head,
        String base,
        String title,
        String body
) {
}
