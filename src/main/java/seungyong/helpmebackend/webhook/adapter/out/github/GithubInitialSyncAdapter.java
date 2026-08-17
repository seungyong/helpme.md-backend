package seungyong.helpmebackend.webhook.adapter.out.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.activity.domain.entity.ActivitySeed;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.global.infrastructure.github.GithubClient;
import seungyong.helpmebackend.global.infrastructure.github.GithubResponseParsingException;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.webhook.application.port.out.InitialSyncPortOut;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GithubInitialSyncAdapter implements InitialSyncPortOut {
    private static final int PAGE_SIZE = 100;

    private final GithubApiExecutor githubApiExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public List<ActivitySeed> fetchActivities(
            Project project,
            String accessToken,
            OffsetDateTime since,
            int maxPages
    ) {
        List<ActivitySeed> result = new ArrayList<>();
        String url = "https://api.github.com/repos/%s/events?per_page=%d"
                .formatted(project.getRepoFullName(), PAGE_SIZE);
        int pageCount = 0;
        boolean reachedPastRange = false;

        while (url != null && pageCount++ < maxPages && !reachedPastRange) {
            JsonPage page = githubApiExecutor.executeGetJson(
                    project.getUserId(),
                    url,
                    accessToken,
                    GithubClient.Accept.APPLICATION_GITHUB_VND_GITHUB_JSON,
                    this::parsePage,
                    "initial project activity sync"
            );
            for (JsonNode event : page.body()) {
                OffsetDateTime occurredAt = parseTime(text(event, "created_at"), null);
                if (occurredAt == null || occurredAt.isBefore(since)) {
                    reachedPastRange = true;
                    break;
                }
                result.addAll(toSeeds(project, event, occurredAt));
            }
            url = page.nextUrl();
        }
        return List.copyOf(result);
    }

    private List<ActivitySeed> toSeeds(
            Project project, JsonNode event, OffsetDateTime occurredAt
    ) {
        String type = text(event, "type");
        if ("PushEvent".equals(type)) {
            return push(project, event, occurredAt);
        }
        if ("PullRequestEvent".equals(type)) {
            ActivitySeed seed = pullRequest(project, event, occurredAt);
            return seed == null ? List.of() : List.of(seed);
        }
        return List.of();
    }

    private List<ActivitySeed> push(
            Project project, JsonNode event, OffsetDateTime occurredAt
    ) {
        JsonNode payload = event.path("payload");
        String ref = text(payload, "ref");
        if (ref == null || !ref.startsWith("refs/heads/")) {
            return List.of();
        }
        String branch = ref.substring("refs/heads/".length());
        if (!isTracked(project, branch) || !payload.path("commits").isArray()
                || payload.path("commits").isEmpty()) {
            return List.of();
        }
        List<ActivitySeed> seeds = new ArrayList<>();
        for (JsonNode commit : payload.path("commits")) {
            String sha = text(commit, "sha");
            String message = text(commit, "message");
            if (sha == null || message == null) {
                continue;
            }
            String[] messageParts = message.split("\\R", 2);
            Map<String, Object> details = new LinkedHashMap<>();
            put(details, "beforeSha", text(payload, "before"));
            put(details, "afterSha", text(payload, "head"));
            details.put("initialSync", true);
            seeds.add(ActivitySeed.builder()
                    .externalKey("commit:" + branch + ":" + sha)
                    .type(ActivityType.PUSH_COMMIT)
                    .branchName(branch)
                    .commitSha(sha)
                    .title(limit(messageParts[0], 500))
                    .summary(messageParts.length == 2 ? limit(messageParts[1], 1000) : null)
                    .actorLogin(text(event.path("actor"), "login"))
                    .publicUrl("https://github.com/%s/commit/%s"
                            .formatted(project.getRepoFullName(), sha))
                    .occurredAt(occurredAt)
                    .details(details)
                    .build());
        }
        return List.copyOf(seeds);
    }

    private ActivitySeed pullRequest(Project project, JsonNode event, OffsetDateTime occurredAt) {
        JsonNode payload = event.path("payload");
        JsonNode pr = payload.path("pull_request");
        String action = text(payload, "action");
        String baseRef = text(pr.path("base"), "ref");
        String headRef = text(pr.path("head"), "ref");
        long number = payload.path("number").asLong(pr.path("number").asLong(-1));
        if (number < 1 || (!isTracked(project, baseRef) && !isTracked(project, headRef))) {
            return null;
        }
        String eventId = text(event, "id");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("prNumber", number);
        put(details, "action", action);
        put(details, "state", text(pr, "state"));
        details.put("draft", pr.path("draft").asBoolean(false));
        details.put("merged", pr.path("merged").asBoolean(false));
        put(details, "headRef", headRef);
        put(details, "headSha", text(pr.path("head"), "sha"));
        put(details, "baseRef", baseRef);
        put(details, "baseSha", text(pr.path("base"), "sha"));
        details.put("initialSync", true);
        return ActivitySeed.builder()
                .externalKey("pr:" + number + ":" + firstNonBlank(action, "snapshot")
                        + ":sync:" + eventId)
                .type(ActivityType.PULL_REQUEST)
                .branchName(baseRef)
                .commitSha(text(pr.path("head"), "sha"))
                .title(firstNonBlank(text(pr, "title"), "PR #" + number))
                .summary("PR #" + number + " " + firstNonBlank(action, "snapshot"))
                .actorLogin(text(event.path("actor"), "login"))
                .publicUrl(text(pr, "html_url"))
                .additions(integer(pr, "additions"))
                .deletions(integer(pr, "deletions"))
                .filesChanged(integer(pr, "changed_files"))
                .occurredAt(occurredAt)
                .details(details)
                .build();
    }

    private JsonPage parsePage(ResponseEntity<String> response) {
        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            if (!body.isArray()) {
                throw new IllegalArgumentException("Repository events response must be an array");
            }
            return new JsonPage(
                    body,
                    GithubClient.extractNextUrl(response.getHeaders()).orElse(null)
            );
        } catch (Exception exception) {
            throw new GithubResponseParsingException(exception);
        }
    }

    private boolean isTracked(Project project, String branch) {
        return branch != null && (project.getSettings().trackAllBranches()
                || branch.equals(project.getDefaultBranch())
                || project.getSettings().trackedBranches().contains(branch));
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isTextual() ? node.path(field).asText() : null;
    }

    private Integer integer(JsonNode node, String field) {
        return node.path(field).canConvertToInt() ? node.path(field).asInt() : null;
    }

    private OffsetDateTime parseTime(String value, OffsetDateTime fallback) {
        try {
            return value == null ? fallback : OffsetDateTime.parse(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private record JsonPage(JsonNode body, String nextUrl) {
    }
}
