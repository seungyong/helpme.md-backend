package seungyong.helpmebackend.repository.adapter.out.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.global.infrastructure.github.GithubClient;
import seungyong.helpmebackend.global.infrastructure.github.GithubResponseParsingException;
import seungyong.helpmebackend.repository.application.port.out.RepositoryQueryPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoPermissionCommand;
import seungyong.helpmebackend.repository.application.port.out.result.ContributorsResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryResult;
import seungyong.helpmebackend.repository.domain.entity.Repository;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RepositoryQueryAdapter implements RepositoryQueryPortOut {
    private static final int MAX_BRANCH_PAGE_REQUESTS = 10;

    private final GithubApiExecutor githubApiExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public RepositoryResult getRepositoriesByInstallationId(
            Long userId,
            String accessToken,
            Long installationId,
            Integer page,
            Integer perPage
    ) {
        String url = String.format(
                "https://api.github.com/user/installations/%d/repositories?per_page=%d&page=%d",
                installationId,
                perPage,
                page
        );

        return GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGet(
                        userId,
                        url,
                        accessToken,
                        jsonNode -> {
                            List<Repository> repositories = new ArrayList<>();
                            for (JsonNode repo : jsonNode.get("repositories")) {
                                repositories.add(new Repository(
                                        repo.get("owner").get("avatar_url").asText(),
                                        repo.get("name").asText(),
                                        repo.get("owner").get("login").asText()
                                ));
                            }
                            return new RepositoryResult(
                                    repositories,
                                    jsonNode.get("total_count").asInt()
                            );
                        },
                        "list installation repositories"
                ),
                GithubRepositoryExceptionTranslator.failWith(
                        RepositoryErrorCode.INSTALLED_REPOSITORY_NOT_FOUND
                )
        );
    }

    @Override
    public RepositoryDetailResult getRepository(RepoInfoCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s",
                command.owner(),
                command.name()
        );

        return GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGet(
                        command.userId(),
                        url,
                        command.accessToken(),
                        jsonNode -> new RepositoryDetailResult(
                                jsonNode.get("owner").get("avatar_url").asText(),
                                command.owner(),
                                command.name(),
                                jsonNode.get("default_branch").asText()
                        ),
                        "get repository"
                ),
                GithubRepositoryExceptionTranslator.failWith(
                        RepositoryErrorCode.REPOSITORY_CANNOT_PULL
                )
        );
    }

    @Override
    public ContributorsResult getContributors(RepoInfoCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/contributors",
                command.owner(),
                command.name()
        );

        return GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGet(
                        command.userId(),
                        url,
                        command.accessToken(),
                        jsonNode -> {
                            List<ContributorsResult.Contributor> contributors = new ArrayList<>();
                            for (JsonNode contributorNode : jsonNode) {
                                if (!"user".equalsIgnoreCase(contributorNode.get("type").asText())) {
                                    continue;
                                }
                                contributors.add(new ContributorsResult.Contributor(
                                        contributorNode.get("login").asText(),
                                        contributorNode.get("avatar_url").asText()
                                ));
                            }
                            return new ContributorsResult(contributors);
                        },
                        "list repository contributors"
                ),
                GithubRepositoryExceptionTranslator.failWith(
                        RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND
                )
        );
    }

    @Override
    public List<RepositoryLanguageResult> getRepositoryLanguages(RepoInfoCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/languages",
                command.owner(),
                command.name()
        );

        return GithubRepositoryExceptionTranslator.execute(() -> githubApiExecutor.executeGet(
                command.userId(),
                url,
                command.accessToken(),
                jsonNode -> {
                    List<RepositoryLanguageResult> languages = new ArrayList<>();
                    for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
                        String language = it.next();
                        languages.add(new RepositoryLanguageResult(
                                language,
                                jsonNode.get(language).asLong()
                        ));
                    }
                    return languages;
                },
                "get repository languages"
        ));
    }

    @Override
    public List<String> getAllBranches(RepoInfoCommand command) {
        Set<String> branches = new LinkedHashSet<>();
        String url = String.format(
                "https://api.github.com/repos/%s/%s/branches?per_page=100",
                command.owner(),
                command.name()
        );
        int requestCount = 0;

        while (url != null) {
            if (++requestCount > MAX_BRANCH_PAGE_REQUESTS) {
                throw new CustomException(
                        RepositoryErrorCode.GITHUB_BRANCHES_TOO_MANY_REQUESTS
                );
            }

            String currentUrl = url;
            url = GithubRepositoryExceptionTranslator.execute(
                    () -> githubApiExecutor.executeGetJson(
                            command.userId(),
                            currentUrl,
                            command.accessToken(),
                            GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_JSON,
                            response -> {
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(response.getBody());
                                    for (JsonNode branchNode : jsonNode) {
                                        branches.add(branchNode.get("name").asText());
                                    }
                                    return GithubClient.extractNextUrl(response.getHeaders()).orElse(null);
                                } catch (Exception e) {
                                    throw new GithubResponseParsingException(e);
                                }
                            },
                            "list repository branches"
                    ),
                    GithubRepositoryExceptionTranslator.failWith(
                            RepositoryErrorCode.REPOSITORY_OR_BRANCH_NOT_FOUND
                    )
            );
        }

        return new ArrayList<>(branches);
    }

    @Override
    public boolean checkPermission(RepoPermissionCommand command) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/collaborators/%s/permission",
                command.repoInfo().owner(),
                command.repoInfo().name(),
                command.username()
        );

        return GithubRepositoryExceptionTranslator.execute(
                () -> githubApiExecutor.executeGet(
                        command.repoInfo().userId(),
                        url,
                        command.repoInfo().accessToken(),
                        jsonNode -> {
                            String permission = jsonNode.get("permission").asText();
                            return "admin".equals(permission) || "write".equals(permission);
                        },
                        "check repository permission"
                ),
                () -> false
        );
    }
}
