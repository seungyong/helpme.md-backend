package seungyong.helpmebackend.repository.application.port.out.result;

public record RepositoryDetailResult(
        String avatarUrl,
        String owner,
        String name,
        String defaultBranch
) {
}
