package seungyong.helpmebackend.github.adapter.out.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.github.application.port.out.GithubAppPortOut;
import seungyong.helpmebackend.github.domain.entity.GithubInstallation;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.entity.GithubRepositoryPage;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.github.domain.type.GithubAccountType;
import seungyong.helpmebackend.github.domain.type.GithubRepositorySelection;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.global.infrastructure.github.GithubClient;
import seungyong.helpmebackend.global.infrastructure.github.GithubResponseParsingException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GithubAppAdapter implements GithubAppPortOut {
    private static final int GITHUB_PAGE_SIZE = 100;
    private static final int MAX_PAGE_REQUESTS = 100;

    private final GithubApiExecutor githubApiExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public List<GithubInstallation> getInstallations(Long userId, String accessToken) {
        return GithubAppExceptionTranslator.installations(() -> {
            List<InstallationSummary> summaries = getAllInstallationSummaries(userId, accessToken);
            List<GithubInstallation> installations = new ArrayList<>();

            for (InstallationSummary summary : summaries) {
                installations.add(new GithubInstallation(
                        summary.installationId(),
                        summary.login(),
                        summary.type(),
                        summary.repositorySelection(),
                        getRepositoryCount(userId, accessToken, summary.installationId())
                ));
            }
            return List.copyOf(installations);
        });
    }

    @Override
    public GithubRepositoryPage getRepositories(
            Long userId,
            String accessToken,
            Long installationId,
            String query,
            int page,
            int size
    ) {
        return GithubAppExceptionTranslator.repositories(() -> {
            if (query == null || query.isBlank()) {
                return getRepositoryPage(
                        userId,
                        accessToken,
                        installationId,
                        page,
                        size
                );
            }
            return searchRepositoryPage(
                    userId,
                    accessToken,
                    installationId,
                    query,
                    page,
                    size
            );
        });
    }

    @Override
    public void validateRepositoryBranches(
            Long userId,
            String accessToken,
            Long installationId,
            Long githubRepositoryId,
            String repositoryFullName,
            Set<String> requiredBranches
    ) {
        GithubAppExceptionTranslator.repositories(() -> {
            if (!isRepositoryAccessibleToInstallation(
                    userId,
                    accessToken,
                    installationId,
                    githubRepositoryId
            )) {
                throw new CustomException(GithubErrorCode.GITHUB_PERMISSION_DENIED);
            }

            Set<String> missingBranches = new HashSet<>(requiredBranches);
            String url = "https://api.github.com/repos/%s/branches?per_page=%d"
                    .formatted(repositoryFullName, GITHUB_PAGE_SIZE);
            int requestCount = 0;

            // missingBranches가 비어있다면 모든 requiredBranches가 존재한다는 의미이므로 반복문 종료
            while (url != null && !missingBranches.isEmpty()) {
                ensurePageLimit(++requestCount);
                JsonPage page = getJsonPage(
                        userId,
                        url,
                        accessToken,
                        "list repository branches"
                );
                if (!page.body().isArray()) {
                    throw parsingFailure("branch response must be an array");
                }
                for (JsonNode branch : page.body()) {
                    // Branch가 requiredBranches에 존재하면 missingBranches에서 제거
                    missingBranches.remove(requiredText(branch, "name"));
                }
                url = page.nextUrl();
            }

            if (!missingBranches.isEmpty()) {
                throw new CustomException(GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND);
            }
            return null;
        });
    }

    private boolean isRepositoryAccessibleToInstallation(
            Long userId,
            String accessToken,
            Long installationId,
            Long githubRepositoryId
    ) {
        String url = "https://api.github.com/user/installations/%d/repositories?per_page=%d"
                .formatted(installationId, GITHUB_PAGE_SIZE);
        int requestCount = 0;

        while (url != null) {
            ensurePageLimit(++requestCount);
            JsonPage page = getJsonPage(
                    userId,
                    url,
                    accessToken,
                    "verify installation repository access"
            );
            for (JsonNode repository : requiredArray(page.body(), "repositories")) {
                if (requiredLong(repository, "id") == githubRepositoryId) {
                    return true;
                }
            }
            url = page.nextUrl();
        }
        return false;
    }

    private GithubRepositoryPage getRepositoryPage(
            Long userId,
            String accessToken,
            Long installationId,
            int page,
            int size
    ) {
        String url = "https://api.github.com/user/installations/%d/repositories?per_page=%d&page=%d"
                .formatted(installationId, size, page);
        JsonPage response = getJsonPage(
                userId,
                url,
                accessToken,
                "list installation repositories"
        );
        List<GithubRepository> repositories = parseRepositorySummaries(response.body()).stream()
                .map(this::toRepository)
                .toList();
        int totalCount = requiredInt(response.body(), "total_count");
        boolean hasNext = (long) page * size < totalCount;

        return new GithubRepositoryPage(
                repositories,
                hasNext ? String.valueOf(page + 1) : null,
                hasNext
        );
    }

    private GithubRepositoryPage searchRepositoryPage(
            Long userId,
            String accessToken,
            Long installationId,
            String query,
            int page,
            int size
    ) {
        List<RepositorySummary> filtered = getAllRepositorySummaries(
                userId,
                accessToken,
                installationId
        ).stream()
                .filter(repository -> matches(repository.fullName(), query))
                .toList();

        long requestedOffset = (long) (page - 1) * size;
        int fromIndex = (int) Math.min(requestedOffset, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<GithubRepository> repositories = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toRepository)
                .toList();
        boolean hasNext = toIndex < filtered.size();

        return new GithubRepositoryPage(
                repositories,
                hasNext ? String.valueOf(page + 1) : null,
                hasNext
        );
    }

    private List<InstallationSummary> getAllInstallationSummaries(
            Long userId,
            String accessToken
    ) {
        List<InstallationSummary> installations = new ArrayList<>();
        String url = "https://api.github.com/user/installations?per_page=" + GITHUB_PAGE_SIZE;
        int requestCount = 0;

        while (url != null) {
            ensurePageLimit(++requestCount);
            JsonPage page = getJsonPage(userId, url, accessToken, "list GitHub installations");
            JsonNode installationNodes = requiredArray(page.body(), "installations");

            for (JsonNode installation : installationNodes) {
                JsonNode account = requiredObject(installation, "account");
                installations.add(new InstallationSummary(
                        requiredLong(installation, "id"),
                        requiredText(account, "login"),
                        GithubAccountType.from(requiredText(account, "type")),
                        GithubRepositorySelection.from(requiredText(
                                installation,
                                "repository_selection"
                        ))
                ));
            }
            url = page.nextUrl();
        }
        return installations;
    }

    private int getRepositoryCount(Long userId, String accessToken, long installationId) {
        String url = "https://api.github.com/user/installations/%d/repositories?per_page=1"
                .formatted(installationId);

        return githubApiExecutor.executeGet(
                userId,
                url,
                accessToken,
                body -> requiredInt(body, "total_count"),
                "count installation repositories"
        );
    }

    private List<RepositorySummary> getAllRepositorySummaries(
            Long userId,
            String accessToken,
            Long installationId
    ) {
        List<RepositorySummary> repositories = new ArrayList<>();
        String url = "https://api.github.com/user/installations/%d/repositories?per_page=%d"
                .formatted(installationId, GITHUB_PAGE_SIZE);
        int requestCount = 0;

        while (url != null) {
            ensurePageLimit(++requestCount);
            JsonPage page = getJsonPage(
                    userId,
                    url,
                    accessToken,
                    "list installation repositories"
            );
            repositories.addAll(parseRepositorySummaries(page.body()));
            url = page.nextUrl();
        }
        return repositories;
    }

    private List<RepositorySummary> parseRepositorySummaries(JsonNode body) {
        List<RepositorySummary> repositories = new ArrayList<>();
        for (JsonNode repository : requiredArray(body, "repositories")) {
            JsonNode permissions = requiredObject(repository, "permissions");
            repositories.add(new RepositorySummary(
                    requiredLong(repository, "id"),
                    requiredText(repository, "full_name"),
                    requiredBoolean(repository, "private"),
                    requiredText(repository, "default_branch"),
                    new GithubRepository.Permissions(
                            requiredBoolean(permissions, "admin"),
                            requiredBoolean(permissions, "push")
                    )
            ));
        }
        return List.copyOf(repositories);
    }

    private GithubRepository toRepository(RepositorySummary summary) {
        return new GithubRepository(
                summary.githubRepoId(),
                summary.fullName(),
                summary.privateRepository(),
                summary.defaultBranch(),
                summary.permissions()
        );
    }

    private JsonPage getJsonPage(
            Long userId,
            String url,
            String accessToken,
            String operationName
    ) {
        return githubApiExecutor.executeGetJson(
                userId,
                url,
                accessToken,
                GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_JSON,
                this::toJsonPage,
                operationName
        );
    }

    private JsonPage toJsonPage(ResponseEntity<String> response) {
        try {
            if (response.getBody() == null) {
                throw new IllegalArgumentException("GitHub response body is null");
            }
            return new JsonPage(
                    objectMapper.readTree(response.getBody()),
                    GithubClient.extractNextUrl(response.getHeaders()).orElse(null)
            );
        } catch (Exception exception) {
            throw new GithubResponseParsingException(exception);
        }
    }

    private boolean matches(String fullName, String query) {
        return (query == null || query.isBlank())
                || fullName.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private void ensurePageLimit(int requestCount) {
        if (requestCount > MAX_PAGE_REQUESTS) {
            throw new CustomException(GithubErrorCode.GITHUB_UPSTREAM_ERROR);
        }
    }

    private JsonNode requiredArray(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isArray()) {
            throw parsingFailure(field + " must be an array");
        }
        return value;
    }

    private JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()) {
            throw parsingFailure(field + " must be an object");
        }
        return value;
    }

    private String requiredText(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isTextual()) {
            throw parsingFailure(field + " must be text");
        }
        return value.asText();
    }

    private long requiredLong(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw parsingFailure(field + " must be a long");
        }
        return value.asLong();
    }

    private int requiredInt(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.canConvertToInt()) {
            throw parsingFailure(field + " must be an integer");
        }
        return value.asInt();
    }

    private boolean requiredBoolean(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isBoolean()) {
            throw parsingFailure(field + " must be boolean");
        }
        return value.asBoolean();
    }

    private GithubResponseParsingException parsingFailure(String message) {
        return new GithubResponseParsingException(new IllegalArgumentException(message));
    }

    private record JsonPage(JsonNode body, String nextUrl) {
    }

    private record InstallationSummary(
            long installationId,
            String login,
            GithubAccountType type,
            GithubRepositorySelection repositorySelection
    ) {
    }

    private record RepositorySummary(
            long githubRepoId,
            String fullName,
            boolean privateRepository,
            String defaultBranch,
            GithubRepository.Permissions permissions
    ) {
    }
}
