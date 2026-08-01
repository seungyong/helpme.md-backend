package seungyong.helpmebackend.repository.adapter.out.github;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.repository.application.port.out.RepositoryMutationPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.CreateBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.out.command.ReadmePushCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RepositoryMutationAdapter implements RepositoryMutationPortOut {
    private final GithubApiExecutor githubApiExecutor;

    @Override
    public void createBranch(CreateBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/refs",
                command.repoInfo().owner(),
                command.repoInfo().name()
        );
        Map<String, String> requestBody = Map.of(
                "ref", "refs/heads/" + command.newBranchName(),
                "sha", command.sha()
        );

        GithubRepositoryExceptionTranslator.execute(() -> githubApiExecutor.executePost(
                command.repoInfo().userId(),
                url,
                command.repoInfo().accessToken(),
                requestBody,
                jsonNode -> null,
                "create repository branch"
        ));
    }

    @Override
    public void deleteBranch(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        GithubRepositoryExceptionTranslator.run(() -> githubApiExecutor.executeDelete(
                command.repoInfo().userId(),
                url,
                command.repoInfo().accessToken(),
                "delete repository branch"
        ));
    }

    @Override
    public void push(ReadmePushCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/contents/README.md",
                command.repoInfo().owner(),
                command.repoInfo().name()
        );

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("message", command.commitMessage());
        requestBody.put(
                "content",
                Base64.getEncoder().encodeToString(
                        command.newContent().getBytes(StandardCharsets.UTF_8)
                )
        );
        requestBody.put("branch", command.branch());
        if (command.readmeSha() != null && !command.readmeSha().isEmpty()) {
            requestBody.put("sha", command.readmeSha());
        }

        GithubRepositoryExceptionTranslator.run(() -> githubApiExecutor.executePut(
                command.repoInfo().userId(),
                url,
                command.repoInfo().accessToken(),
                requestBody,
                "push README"
        ));
    }

    @Override
    public String createPullRequest(CreatePullRequestCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/pulls",
                command.repoInfo().owner(),
                command.repoInfo().name()
        );
        Map<String, String> requestBody = Map.of(
                "head", command.head(),
                "base", command.base(),
                "title", command.title(),
                "body", command.body()
        );

        return GithubRepositoryExceptionTranslator.execute(() -> githubApiExecutor.executePost(
                command.repoInfo().userId(),
                url,
                command.repoInfo().accessToken(),
                requestBody,
                jsonNode -> jsonNode.get("html_url").asText(),
                "create README pull request"
        ));
    }
}
