package seungyong.helpmebackend.adapter.out.result;

public record RepositoryDetailResult(
        String avatarUrl,
        String name,
        String owner,
        String defaultBranch
) {
}
