package seungyong.helpmebackend.adapter.out.repository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.adapter.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.repository.Repository;
import seungyong.helpmebackend.infrastructure.github.GithubClient;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;

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
    public List<String> getCommitsByBranch(String accessToken, String owner, String name, String branch) {
        List<String> commits = new ArrayList<>();

        for (int page = 1; page <= 2; page++) {
            String url = String.format("https://api.github.com/repos/%s/%s/commits?sha=%s&per_page=100&page=%s", owner, name, branch, page);
            String responseBody = githubClient.fetchGetMethodForBody(url, accessToken);

            try {
                JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
                if (jsonNode.isEmpty()) { break; }

                for (JsonNode commitNode : jsonNode) {
                    commits.add(commitNode.get("commit").get("message").asText());
                }

                if (jsonNode.size() < 100) { break; }
            } catch (Exception e) {
                log.error("Error parsing Github commits response = {}", responseBody, e);
                throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
            }
        }

        return commits;
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
}
