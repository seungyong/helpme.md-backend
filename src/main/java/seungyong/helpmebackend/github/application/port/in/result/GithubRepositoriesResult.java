package seungyong.helpmebackend.github.application.port.in.result;

import seungyong.helpmebackend.github.domain.entity.GithubRepository;

import java.util.List;

public record GithubRepositoriesResult(
        List<Item> items,
        Page page
) {
    public GithubRepositoriesResult {
        items = List.copyOf(items);
    }

    public record Item(
            GithubRepository repository,
            boolean alreadyConnected
    ) {
    }

    public record Page(
            String nextCursor,
            boolean hasNext
    ) {
    }
}
