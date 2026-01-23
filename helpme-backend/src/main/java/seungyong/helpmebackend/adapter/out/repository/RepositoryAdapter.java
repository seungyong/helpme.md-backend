package seungyong.helpmebackend.adapter.out.repository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.adapter.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.adapter.out.command.ReadmePushCommand;
import seungyong.helpmebackend.adapter.out.result.*;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.repository.Repository;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;
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
    public String getDefaultBranch(String accessToken, String owner, String name) {
        String url = String.format("https://api.github.com/repos/%s/%s", owner, name);
        String responseBody = githubClient.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode repo = super.getObjectMapper().readTree(responseBody);
            return repo.get("default_branch").asText();
        } catch (Exception e) {
            log.error("Error parsing Github repository response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public String getRecentSHA(String accessToken, String owner, String name, String branch) {
        String url = String.format("https://api.github.com/repos/%s/%s/git/refs/heads/%s", owner, name, branch);
        String responseBody = githubClient.fetchGetMethodForBody(url, accessToken);

        try {
            JsonNode ref = super.getObjectMapper().readTree(responseBody);
            return ref.get("object").get("sha").asText();
        } catch (Exception e) {
            log.error("Error parsing Github recent SHA response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public void createBranch(String accessToken, String owner, String name, String newBranchName, String sha) {
        String url = String.format("https://api.github.com/repos/%s/%s/git/refs", owner, name);
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("ref", "refs/heads/" + newBranchName);
        requestBody.put("sha", sha);

        try {
            githubClient.postWithBearer(url, accessToken, requestBody, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Error creating branch {} in Github repository {}/{}", newBranchName, owner, name, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public void deleteBranch(String accessToken, String owner, String name, String branch) {
        String url = String.format("https://api.github.com/repos/%s/%s/git/refs/heads/%s", owner, name, branch);

        try {
            githubClient.deleteWithBearer(url, accessToken);
        } catch (HttpClientErrorException e) {
            log.error("Error deleting branch {} in Github repository {}/{}", branch, owner, name, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public String getReadmeSHA(String accessToken, String owner, String name, String branch) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/README.md?ref=%s", owner, name, branch);

        String responseBody;
        try {
            responseBody = githubClient.fetchGetMethodForBody(url, accessToken);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }

        try {
            JsonNode readme = super.getObjectMapper().readTree(responseBody);
            return readme.get("sha").asText();
        } catch (Exception e) {
            log.error("Error parsing Github readme SHA response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public void push(ReadmePushCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                "README.md"
        );

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("message", command.commitMessage());
        requestBody.put("content", Base64.getEncoder().encodeToString(command.newContent().getBytes()));
        requestBody.put("branch", command.branch());

        // 기존 README가 있는 경우에만 sha 포함
        if (command.readmeSha() != null) {
            requestBody.put("sha", command.readmeSha());
        }

        try {
            githubClient.putWithBearer(url, command.repoInfo().accessToken(), requestBody, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Error pushing README to Github repository {}/{} on branch {}", command.repoInfo().owner(), command.repoInfo().name(), command.branch(), e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    @Override
    public String createPullRequest(CreatePullRequestCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/pulls",
                command.repoInfo().owner(),
                command.repoInfo().name()
        );

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("head", command.head());
        requestBody.put("base", command.base());
        requestBody.put("title", command.title());
        requestBody.put("body", command.body());

        String responseBody;
        try {
            responseBody = githubClient.postWithBearer(url, command.repoInfo().accessToken(), requestBody, String.class);
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.error(
                    "Error creating Github pull request for {}/{} from {} to {}: {}",
                    command.repoInfo().owner(),
                    command.repoInfo().name(),
                    command.head(),
                    command.base(),
                    e.getResponseBodyAsString()
            );
            throw new CustomException(RepositoryErrorCode.BAD_REQUEST_SAME_BRANCH);
        } catch (HttpClientErrorException e) {
            log.error(
                    "Error creating Github pull request for {}/{} from {} to {}",
                    command.repoInfo().owner(),
                    command.repoInfo().name(),
                    command.head(),
                    command.base(),
                    e
            );
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }

        try {
            JsonNode pr = super.getObjectMapper().readTree(responseBody);
            return pr.get("html_url").asText();
        } catch (Exception e) {
            log.error("Error parsing Github pull request response = {}", responseBody, e);
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
