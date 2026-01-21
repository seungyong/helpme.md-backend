package seungyong.helpmebackend.adapter.out.repository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.adapter.out.result.*;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.repository.Repository;
import seungyong.helpmebackend.infrastructure.github.GithubClient;
import seungyong.helpmebackend.infrastructure.github.dto.PageInfo;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryAdapter extends GithubPortConfig implements RepositoryPortOut {
    private final GithubClient githubClient;

    @Override
    public RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page) {
        String url = String.format("https://api.github.com/user/installations/%d/repositories?per_page=30&page=%d", installationId, page);
        String responseBody = githubClient.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
            List<Repository> repositories = new ArrayList<>();

            for (JsonNode repo : jsonNode.get("repositories")) {
                String avatarUrl = repo.get("owner").get("avatar_url").asText();
                String name = repo.get("name").asText();
                String owner = repo.get("owner").get("login").asText();

                repositories.add(new Repository(avatarUrl, name, owner));
            }

            return new RepositoryResult(repositories, jsonNode.get("total_count").asInt());
        } catch (Exception e) {
            log.error("Error parsing Github intsallations response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public RepositoryDetailResult getRepository(String accessToken, String owner, String name) {
        String url = String.format("https://api.github.com/repos/%s/%s", owner, name);
        String responseBody = githubClient.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode repo = super.getObjectMapper().readTree(responseBody);

            String avatarUrl = repo.get("owner").get("avatar_url").asText();
            String defaultBranch = repo.get("default_branch").asText();

            return new RepositoryDetailResult(avatarUrl, name, owner, defaultBranch);
        } catch (Exception e) {
            log.error("Error parsing Github repository response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public List<RepositoryLanguageResult> getRepositoryLanguages(String accessToken, String owner, String name) {
        String url = String.format("https://api.github.com/repos/%s/%s/languages", owner, name);
        String responseBody = githubClient.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
            List<RepositoryLanguageResult> languages = new ArrayList<>();

            for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
                String language = it.next();
                long bytes = jsonNode.get(language).asLong();
                languages.add(new RepositoryLanguageResult(language, bytes));
            }

            return languages;
        } catch (Exception e) {
            log.error("Error parsing Github repository languages response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public String getReadmeContent(String accessToken, String owner, String name, String branch) {
        String url = String.format("https://api.github.com/repos/%s/%s/readme?ref=%s", owner, name, branch);

        try {
            return githubClient.fetchGetMethodForBody(url, accessToken, GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_RAW_JSON);
        } catch (HttpClientErrorException.NotFound e) {
            return "";
        }
    }

    @Override
    public List<String> getAllBranches(String accessToken, String owner, String name) {
        Set<String> branches = new LinkedHashSet<>();
        String url = String.format("https://api.github.com/repos/%s/%s/branches?per_page=1", owner, name);

        final int MAX_REQUIRES = 10;
        int requestCount = 0;

        while (url != null) {
            if (++requestCount > MAX_REQUIRES) {
                log.error("Exceeded maximum number of requests to fetch all branches for {}/{}", owner, name);
                throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
            }

            ResponseEntity<String> response = githubClient.fetchGet(url, accessToken, GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_RAW_JSON, String.class);
            String responseBody = response.getBody();

            try {
                JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
                for (JsonNode branchNode : jsonNode) {
                    branches.add(branchNode.get("name").asText());
                }

                url = GithubClient.extractNextUrl(response.getHeaders())
                        .orElse(null);
            } catch (Exception e) {
                log.error("Error parsing Github branches response = {}", responseBody, e);
                throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
            }
        }

        return new ArrayList<>(branches);
    }

    @Override
    public Optional<CommitResult> getCommitsByBranch(String accessToken, String owner, String name, String branch) {
        int page = 1;

        try {
            ResponseEntity<String> latestResponse = fetchCommit(accessToken, owner, name, branch, page);
            String latestResponseBody = latestResponse.getBody();

            PageInfo link = GithubClient.extractLastAndMiddlePage(latestResponse.getHeaders());

            // 커밋이 한 페이지에 모두 있는 경우
            if (
                    link.lastPage() == null || link.middlePage() == null ||
                    Objects.equals(link.lastPage(), link.middlePage()) ||
                    link.lastPage() == 1 || link.middlePage() == 1
            ) {
                return Optional.of(new CommitResult(
                        parseJsonCommit(latestResponseBody),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
            }

            List<CommitResult.Commit> latestCommits = parseJsonCommit(latestResponseBody);

            // 중간 페이지와 마지막 페이지를 모두 가져오기
            ResponseEntity<String> middleResponse = fetchCommit(accessToken, owner, name, branch, link.middlePage());
            List<CommitResult.Commit> middleCommits = parseJsonCommit(middleResponse.getBody());

            ResponseEntity<String> initialResponse = fetchCommit(accessToken, owner, name, branch, link.lastPage());
            List<CommitResult.Commit> initialCommits = parseJsonCommit(initialResponse.getBody());

            // 30개보다 적은 경우 추가 요청하여 30개 맞추기
            if (initialCommits.size() < 30) {
                ResponseEntity<String> penultimateResponse = fetchCommit(accessToken, owner, name, branch, link.lastPage() - 1);
                List<CommitResult.Commit> penultimateCommits = parseJsonCommit(penultimateResponse.getBody());
                int needed = 30 - initialCommits.size();

                for (int i = penultimateCommits.size() - 1; i >= 0 && needed > 0; i--, needed--) {
                    initialCommits.add(0, penultimateCommits.get(i));
                }
            }

            return Optional.of(new CommitResult(
                    latestCommits,
                    initialCommits,
                    middleCommits
            ));
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException.NotFound) {
                return Optional.empty();
            }

            log.error("Error fetching Github commits for {}/{} on branch {}", owner, name, branch, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public List<RepositoryTreeResult> getRepositoryTree(String accessToken, String owner, String name, String branch) {
        String url = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1", owner, name, branch);
        String responseBody = githubClient.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
            List<RepositoryTreeResult> treeResults = new ArrayList<>();

            for (JsonNode treeNode : jsonNode.get("tree")) {
                String path = treeNode.get("path").asText();
                String type = treeNode.get("type").asText();

                treeResults.add(new RepositoryTreeResult(path, type));
            }

            return treeResults;
        } catch (Exception e) {
            log.error("Error parsing Github repository tree response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public RepositoryFileContentResult getFileContent(String accessToken, String owner, String name, RepositoryTreeResult file) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, name, file.path());

        try {
            return new RepositoryFileContentResult(
                    file.path(),
                    githubClient.fetchGetMethodForBody(url, accessToken, GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_RAW_JSON)
            );
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    private ResponseEntity<String> fetchCommit(String accessToken, String owner, String name, String branch, int page) {
        String url = String.format("https://api.github.com/repos/%s/%s/commits?sha=%s&page=%s", owner, name, branch, page);
        return githubClient.fetchGet(url, accessToken, GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_JSON, String.class);
    }

    private List<CommitResult.Commit> parseJsonCommit(String body) {
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
        } catch (Exception e) {
            log.error("Error parsing Github commits response = {}", body, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }
}
