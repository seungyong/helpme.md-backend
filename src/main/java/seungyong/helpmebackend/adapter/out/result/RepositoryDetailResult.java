package seungyong.helpmebackend.adapter.out.result;

public record RepositoryDetailResult(
        String avatarUrl,
        String owner,
        String name,
        String defaultBranch
) {
}
