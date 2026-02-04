package seungyong.helpmebackend.adapter.out.command;

public record RepoPermissionCommand(
        RepoInfoCommand repoInfo,
        String username
) {
}
