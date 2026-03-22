package seungyong.helpmebackend.repository.application.port.out.command;

public record RepoInfoCommand(
        String accessToken,
        String owner,
        String name
) {
}
