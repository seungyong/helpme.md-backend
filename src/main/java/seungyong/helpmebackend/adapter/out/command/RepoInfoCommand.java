package seungyong.helpmebackend.adapter.out.command;

public record RepoInfoCommand(
        String accessToken,
        String owner,
        String name
) {
}
