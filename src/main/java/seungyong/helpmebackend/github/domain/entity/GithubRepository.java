package seungyong.helpmebackend.github.domain.entity;

public record GithubRepository(
        long githubRepoId,
        String fullName,
        boolean privateRepository,
        String defaultBranch,
        Permissions permissions
) {
    public record Permissions(boolean admin, boolean push) {
    }
}
