package seungyong.helpmebackend.repository.application.port.in.result;

public record RepositoryDetailsResult(
        String owner,
        String name,
        String avatarUrl,
        String defaultBranch
) {
}
