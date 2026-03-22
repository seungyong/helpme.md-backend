package seungyong.helpmebackend.repository.application.port.out.command;

public record RepoPermissionCommand(
        RepoInfoCommand repoInfo,
        String username
) {
}
