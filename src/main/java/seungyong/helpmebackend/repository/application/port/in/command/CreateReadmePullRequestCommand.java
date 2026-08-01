package seungyong.helpmebackend.repository.application.port.in.command;

public record CreateReadmePullRequestCommand(
        Long userId,
        String owner,
        String name,
        String branch,
        String content
) {
}
