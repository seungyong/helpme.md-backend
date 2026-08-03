package seungyong.helpmebackend.github.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import seungyong.helpmebackend.github.application.port.in.result.GithubRepositoriesResult;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;

import java.util.List;

@Schema(description = "GitHub App installation의 연결 후보 Repository 목록")
public record ResponseGithubRepositories(
        List<Item> items,
        Page page
) {
    public static ResponseGithubRepositories from(GithubRepositoriesResult result) {
        return new ResponseGithubRepositories(
                result.items().stream().map(Item::from).toList(),
                new Page(result.page().nextCursor(), result.page().hasNext())
        );
    }

    public record Item(
            long githubRepoId,
            String fullName,
            boolean isPrivate,
            String defaultBranch,
            Permissions permissions,
            boolean alreadyConnected
    ) {
        private static Item from(GithubRepositoriesResult.Item item) {
            GithubRepository repository = item.repository();
            return new Item(
                    repository.githubRepoId(),
                    repository.fullName(),
                    repository.privateRepository(),
                    repository.defaultBranch(),
                    new Permissions(
                            repository.permissions().admin(),
                            repository.permissions().push()
                    ),
                    item.alreadyConnected()
            );
        }
    }

    public record Permissions(boolean admin, boolean push) {
    }

    public record Page(String nextCursor, boolean hasNext) {
    }
}
