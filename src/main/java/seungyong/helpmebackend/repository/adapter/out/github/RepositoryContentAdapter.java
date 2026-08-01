package seungyong.helpmebackend.repository.adapter.out.github;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.repository.application.port.out.RepositoryContentPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RepositoryContentAdapter implements RepositoryContentPortOut {
    private final GithubApiExecutor githubApiExecutor;

    @Override
    public String getRecentSHA(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        return GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGet(
                        command.repoInfo().userId(),
                        url,
                        command.repoInfo().accessToken(),
                        jsonNode -> jsonNode.get("object").get("sha").asText(),
                        "get latest branch SHA"
                ),
                GithubRepositoryExceptionTranslator.failWith(RepositoryErrorCode.BRANCH_NOT_FOUND)
        );
    }

    @Override
    public String getReadmeSHA(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/contents/README.md?ref=%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        String sha = GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGet(
                        command.repoInfo().userId(),
                        url,
                        command.repoInfo().accessToken(),
                        jsonNode -> jsonNode.get("sha").asText(),
                        "get README SHA"
                ),
                () -> ""
        );
        return sha.isEmpty() ? null : sha;
    }

    @Override
    public String getReadmeContent(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/readme?ref=%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        return GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGetRaw(
                        command.repoInfo().userId(),
                        url,
                        command.repoInfo().accessToken(),
                        "get README content"
                ),
                () -> ""
        );
    }

    @Override
    public List<RepositoryTreeResult> getRepositoryTree(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        return GithubRepositoryExceptionTranslator.execute(() -> githubApiExecutor.executeGet(
                command.repoInfo().userId(),
                url,
                command.repoInfo().accessToken(),
                jsonNode -> {
                    List<RepositoryTreeResult> results = new ArrayList<>();
                    for (var treeNode : jsonNode.get("tree")) {
                        results.add(new RepositoryTreeResult(
                                treeNode.get("path").asText(),
                                treeNode.get("type").asText()
                        ));
                    }
                    return results;
                },
                "get repository tree"
        ));
    }

    @Override
    public RepositoryFileContentResult getFileContent(
            RepoBranchCommand command,
            RepositoryTreeResult file
    ) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                file.path(),
                command.branch()
        );

        String content = GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGetRaw(
                        command.repoInfo().userId(),
                        url,
                        command.repoInfo().accessToken(),
                        "get repository file content"
                ),
                () -> ""
        );
        return new RepositoryFileContentResult(file.path(), content);
    }
}
