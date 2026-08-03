package seungyong.helpmebackend.github.domain.entity;

import java.util.List;

public record GithubRepositoryPage(
        List<GithubRepository> repositories,
        String nextCursor,
        boolean hasNext
) {
    public GithubRepositoryPage {
        repositories = List.copyOf(repositories);
    }
}
