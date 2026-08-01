package seungyong.helpmebackend.repository.application.port.out.command;

public record RepoInfoCommand(
        Long userId,
        String accessToken,
        String owner,
        String name
) {
}
