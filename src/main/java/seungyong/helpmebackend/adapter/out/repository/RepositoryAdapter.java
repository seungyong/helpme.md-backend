package seungyong.helpmebackend.adapter.out.repository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.adapter.out.command.*;
import seungyong.helpmebackend.adapter.out.result.*;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.domain.entity.repository.Repository;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.infrastructure.github.GithubClient;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryAdapter extends GithubPortConfig implements RepositoryPortOut {
    private final GithubApiExecutor githubApiExecutor;

    @Override
    public RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page, Integer perPage) {
        String url = String.format("https://api.github.com/user/installations/%d/repositories?per_page=%d&page=%d", installationId, perPage, page);

        return githubApiExecutor.executeGet(
                url,
                accessToken,
                jsonNode -> {
                    List<Repository> repositories = new ArrayList<>();

                    for (JsonNode repo : jsonNode.get("repositories")) {
                        String avatarUrl = repo.get("owner").get("avatar_url").asText();
                        String name = repo.get("name").asText();
                        String owner = repo.get("owner").get("login").asText();

                        repositories.add(new Repository(avatarUrl, name, owner));
                    }

                    return new RepositoryResult(repositories, jsonNode.get("total_count").asInt());
                },
                "Get repositories by installation_id = " + installationId,
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        throw new CustomException(RepositoryErrorCode.INSTALLED_REPOSITORY_NOT_FOUND);
                    }

                    return Optional.empty();
                }
        );
    }

    @Override
    public RepositoryDetailResult getRepository(RepoInfoCommand command) {
        String url = String.format("https://api.github.com/repos/%s/%s", command.owner(), command.name());

        return githubApiExecutor.executeGet(
                url,
                command.accessToken(),
                jsonNode -> {
                    String avatarUrl = jsonNode.get("owner").get("avatar_url").asText();
                    String defaultBranch = jsonNode.get("default_branch").asText();

                    return new RepositoryDetailResult(avatarUrl, command.owner(), command.name(), defaultBranch);
                },
                "Get repository info " + command.owner() + "/" + command.name(),
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        throw new CustomException(RepositoryErrorCode.REPOSITORY_CANNOT_PULL);
                    }

                    return Optional.empty();
                }
        );
    }

    @Override
    public ContributorsResult getContributors(RepoInfoCommand info) {
        String url = String.format("https://api.github.com/repos/%s/%s/contributors", info.owner(), info.name());

return githubApiExecutor.executeGet(
                url,
                info.accessToken(),
                jsonNode -> {
                    List<ContributorsResult.Contributor> contributors = new ArrayList<>();

                    for (JsonNode contributorNode : jsonNode) {
                        String type = contributorNode.get("type").asText().toLowerCase();
                        // 사용자가 아닌 경우 (예: Bot, Organization) 건너뛰기
                        if (!type.equals("user")) {
                            continue;
                        }

                        String username = contributorNode.get("login").asText();
                        String avatarUrl = contributorNode.get("avatar_url").asText();

                        contributors.add(new ContributorsResult.Contributor(username, avatarUrl));
                    }

                    return new ContributorsResult(contributors);
                },
                "Get contributors for " + info.owner() + "/" + info.name(),
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        throw new CustomException(RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND);
                    }

                    return Optional.empty();
                }
        );
    }

    @Override
    public String getRecentSHA(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        return githubApiExecutor.executeGet(
                url,
                command.repoInfo().accessToken(),
                jsonNode -> jsonNode.get("object").get("sha").asText(),
                "Get recent SHA for " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " on branch " + command.branch(),
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        log.error(
                                "Branch {} not found in Github repository {}/{}",
                                command.branch(),
                                command.repoInfo().owner(),
                                command.repoInfo().name(),
                                e
                        );
                        throw new CustomException(RepositoryErrorCode.BRANCH_NOT_FOUND);
                    }

                    return Optional.empty();
                }
        );
    }

    @Override
    public void createBranch(CreateBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/refs",
                command.repoInfo().owner(),
                command.repoInfo().name()
        );
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("ref", "refs/heads/" + command.newBranchName());
        requestBody.put("sha", command.sha());

        githubApiExecutor.executePost(
                url,
                command.repoInfo().accessToken(),
                requestBody,
                jsonNode -> null,
                "Create branch " + command.newBranchName() + " in " + command.repoInfo().owner() + "/" + command.repoInfo().name()
        );
    }

    @Override
    public void deleteBranch(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/refs/heads/%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        githubApiExecutor.executeDelete(
                url,
                command.repoInfo().accessToken(),
                "Delete branch " + command.branch() + " in " + command.repoInfo().owner() + "/" + command.repoInfo().name()
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

        String sha = githubApiExecutor.executeGet(
                url,
                command.repoInfo().accessToken(),
                jsonNode -> jsonNode.get("sha").asText(),
                "Get README SHA for " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " on branch " + command.branch(),
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        return Optional.of("");
                    }

                    return Optional.empty();
                }
        );

        if (sha.isEmpty()) {
            return null;
        }

        return sha;
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
        if (command.readmeSha() != null && !command.readmeSha().isEmpty()) {
            requestBody.put("sha", command.readmeSha());
        }

        githubApiExecutor.executePut(
                url,
                command.repoInfo().accessToken(),
                requestBody,
                "Push README to " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " on branch " + command.branch()
        );
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

        return githubApiExecutor.executePost(
                url,
                command.repoInfo().accessToken(),
                requestBody,
                jsonNode -> jsonNode.get("html_url").asText(),
                "Create pull request for " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " from " + command.head() + " to " + command.base()
        );
    }

    @Override
    public List<RepositoryLanguageResult> getRepositoryLanguages(RepoInfoCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/languages",
                command.owner(),
                command.name()
        );

        return githubApiExecutor.executeGet(
                url,
                command.accessToken(),
                jsonNode -> {
                    List<RepositoryLanguageResult> languages = new ArrayList<>();

                    for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
                        String language = it.next();
                        long bytes = jsonNode.get(language).asLong();
                        languages.add(new RepositoryLanguageResult(language, bytes));
                    }

                    return languages;
                },
                "Get repository languages for " + command.owner() + "/" + command.name()
        );
    }

    @Override
    public String getReadmeContent(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/readme?ref=%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        return githubApiExecutor.executeGetRaw(
                url,
                command.repoInfo().accessToken(),
                "Get README content for " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " on branch " + command.branch(),
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        return Optional.of("");
                    }

                    return Optional.empty();
                }
        );
    }

    @Override
    public List<String> getAllBranches(RepoInfoCommand command) {
        Set<String> branches = new LinkedHashSet<>();
        String url = String.format(
                "https://api.github.com/repos/%s/%s/branches?per_page=1",
                command.owner(),
                command.name()
        );

        final int MAX_REQUESTS = 10;
        int requestCount = 0;

        while (url != null) {
            if (++requestCount > MAX_REQUESTS) {
                log.error(
                        "Exceeded maximum number of requests to fetch all branches for {}/{}",
                        command.owner(),
                        command.name()
                );
                throw new CustomException(RepositoryErrorCode.GITHUB_BRANCHES_TOO_MANY_REQUESTS);
            }

            String currentUrl = url;

            url = githubApiExecutor.executeGetJson(
                    currentUrl,
                    command.accessToken(),
                    GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_RAW_JSON,
                    response -> {
                        try {
                            String body = response.getBody();
                            JsonNode jsonNode = super.getObjectMapper().readTree(body);

                            for (JsonNode branchNode : jsonNode) {
                                String branchName = branchNode.get("name").asText();
                                branches.add(branchName);
                            }

                            return GithubClient.extractNextUrl(response.getHeaders())
                                    .orElse(null);
                        } catch (Exception e) {
                            throw new CustomException(RepositoryErrorCode.JSON_PROCESSING_ERROR);
                        }
                    },
                    "Get all branches for " + command.owner() + "/" + command.name(),
                    e -> {
                        if (e instanceof HttpClientErrorException.NotFound) {
                            throw new CustomException(RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND);
                        }

                        return Optional.empty();
                    }
            );
        }

        return new ArrayList<>(branches);
    }

    @Override
    public List<RepositoryTreeResult> getRepositoryTree(RepoBranchCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.branch()
        );

        return githubApiExecutor.executeGet(
                url,
                command.repoInfo().accessToken(),
                jsonNode -> {
                    List<RepositoryTreeResult> results = new ArrayList<>();

                    for (JsonNode treeNode : jsonNode.get("tree")) {
                        String path = treeNode.get("path").asText();
                        String type = treeNode.get("type").asText();

                        results.add(new RepositoryTreeResult(path, type));
                    }

                    return results;
                },
                "Get repository tree for " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " on branch " + command.branch()
        );
    }

    @Override
    public RepositoryFileContentResult getFileContent(RepoBranchCommand command, RepositoryTreeResult file) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                file.path(),
                command.branch()
        );

        String content = githubApiExecutor.executeGetRaw(
                url,
                command.repoInfo().accessToken(),
                "Get file content for " + file.path() + " in " + command.repoInfo().owner() + "/" + command.repoInfo().name() + " on branch " + command.branch(),
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        return Optional.of("");
                    }

                    return Optional.empty();
                }
        );

        return new RepositoryFileContentResult(file.path(), content);
    }

    @Override
    public boolean checkPermission(RepoPermissionCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/collaborators/%s/permission",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.username()
        );

        return githubApiExecutor.executeGet(
                url,
                command.repoInfo().accessToken(),
                jsonNode -> {
                    String permission = jsonNode.get("permission").asText();
                    return permission.equals("admin") || permission.equals("write");
                },
                "Check permission for user " + command.username() + " in " + command.repoInfo().owner() + "/" + command.repoInfo().name(),
                e -> {
                    if (e instanceof HttpClientErrorException.NotFound) {
                        return Optional.of(false);
                    }

                    return Optional.empty();
                }
        );
    }
}
