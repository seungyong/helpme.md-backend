package seungyong.helpmebackend.repository.application.port.out.command;

public record CreatePullRequestCommand(
        RepoInfoCommand repoInfo,
        String head,
        String base,
        String title,
        String body
) {
}
