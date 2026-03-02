package seungyong.helpmebackend.adapter.out.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.adapter.out.command.RepoBranchCommand;
import seungyong.helpmebackend.adapter.out.result.CommitResult;
import seungyong.helpmebackend.adapter.out.result.ContributorsResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.infrastructure.github.GithubClient;
import seungyong.helpmebackend.infrastructure.github.dto.PageInfo;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.repository.CommitPortOut;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommitAdapter extends GithubPortConfig implements CommitPortOut {
    private final GithubApiExecutor githubApiExecutor;

    @Override
    public CommitResult getCommits(RepoBranchCommand command, ContributorsResult.Contributor contributor) {
        int page = 1;

        ResponseEntity<String> latestResponse = fetchCommit(command, page, contributor.username());
        String latestResponseBody = latestResponse.getBody();

        PageInfo link = GithubClient.extractLastAndMiddlePage(latestResponse.getHeaders());

        // 커밋이 한 페이지에 모두 있는 경우
        if (
                link.lastPage() == null || link.middlePage() == null ||
                        Objects.equals(link.lastPage(), link.middlePage()) ||
                        link.lastPage() == 1
        ) {
            return new CommitResult(
                    contributor,
                    parseJsonCommit(latestResponseBody),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        List<CommitResult.Commit> latestCommits = parseJsonCommit(latestResponseBody);

        // 중간 페이지와 마지막 페이지를 모두 가져오기
        CompletableFuture<List<CommitResult.Commit>> middleFuture = CompletableFuture.supplyAsync(() -> {
            ResponseEntity<String> middleResponse = fetchCommit(command, link.middlePage(), contributor.username());
            return parseJsonCommit(middleResponse.getBody());
        });

        CompletableFuture<List<CommitResult.Commit>> initialFuture = CompletableFuture.supplyAsync(() -> {
            ResponseEntity<String> initialResponse = fetchCommit(command, link.lastPage(), contributor.username());
            List<CommitResult.Commit> initialCommits = parseJsonCommit(initialResponse.getBody());

            // 30개보다 적은 경우 추가 요청하여 30개 맞추기
            if (initialCommits.size() < 30 && link.lastPage() > 1) {
                ResponseEntity<String> penultimateResponse = fetchCommit(command, link.lastPage() - 1, contributor.username());
                List<CommitResult.Commit> penultimateCommits = parseJsonCommit(penultimateResponse.getBody());
                int needed = 30 - initialCommits.size();

                for (int i = penultimateCommits.size() - 1; i >= 0 && needed > 0; i--, needed--) {
                    initialCommits.add(0, penultimateCommits.get(i));
                }
            }

            return initialCommits;
        });

        List<CommitResult.Commit> middleCommits = middleFuture.join();
        List<CommitResult.Commit> initialCommits = initialFuture.join();

        return new CommitResult(
                contributor,
                latestCommits,
                initialCommits,
                middleCommits
        );
    }

    private ResponseEntity<String> fetchCommit(RepoBranchCommand command, int page, String contributor) {
        String url;

        if (contributor == null || contributor.isEmpty()) {
            url = String.format(
                    "https://api.github.com/repos/%s/%s/commits?sha=%s&per_page=40&page=%d",
                    command.repoInfo().owner(),
                    command.repoInfo().name(),
                    command.branch(),
                    page
            );
        } else {
            url = String.format(
                    "https://api.github.com/repos/%s/%s/commits?sha=%s&per_page=20&page=%d&author=%s",
                    command.repoInfo().owner(),
                    command.repoInfo().name(),
                    command.branch(),
                    page,
                    contributor
            );
        }

        return githubApiExecutor.executeGetJson(
                url,
                command.repoInfo().accessToken(),
                GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_JSON,
                response -> response,
                "Fetch commits for " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " on branch " + command.branch() + " page " + page,
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        throw new CustomException(RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND);
                    }

                    return Optional.empty();
                }
        );
    }

    private List<CommitResult.Commit> parseJsonCommit(String body) {
        if (body == null || body.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(body);
            List<CommitResult.Commit> commits = new ArrayList<>();

            for (JsonNode commitNode : jsonNode) {
                String sha = commitNode.get("sha").asText();
                String message = commitNode.get("commit").get("message").asText();
                Instant date = Instant.parse(commitNode.get("commit").get("committer").get("date").asText());

                commits.add(new CommitResult.Commit(
                        sha,
                        message,
                        date
                ));
            }

            return commits;
        } catch (JsonProcessingException e) {
            log.error("Error parsing Github commit JSON response = {}", body, e);
            throw new CustomException(RepositoryErrorCode.JSON_PROCESSING_ERROR);
        }
    }
}
