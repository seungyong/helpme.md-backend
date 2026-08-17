package seungyong.helpmebackend.webhook.application.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.activity.domain.entity.ActivitySeed;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WebhookActivitySeedFactory {
    private static final String HEADS_PREFIX = "refs/heads/";
    private static final Set<String> SUPPORTED_PR_ACTIONS = Set.of(
            "opened", "reopened", "synchronize", "ready_for_review", "closed"
    );
    private final ObjectMapper objectMapper;

    public Result create(
            Project project,
            String eventName,
            String deliveryId,
            Map<String, Object> payload,
            OffsetDateTime receivedAt
    ) {
        JsonNode root = objectMapper.valueToTree(payload);
        return switch (eventName) {
            case "push" -> push(project, root, receivedAt);
            case "pull_request" -> pullRequest(project, deliveryId, root, receivedAt);
            case "ping" -> new Result(List.of(), false);
            default -> new Result(List.of(), true);
        };
    }

    private Result push(Project project, JsonNode root, OffsetDateTime receivedAt) {
        String ref = text(root, "ref");
        if (ref == null || !ref.startsWith(HEADS_PREFIX)) {
            return new Result(List.of(), true);
        }
        String branch = ref.substring(HEADS_PREFIX.length());
        if (!isTracked(project, branch)) {
            return new Result(List.of(), true);
        }
        List<ActivitySeed> seeds = new ArrayList<>();
        if (root.path("commits").isArray()) {
            for (JsonNode commit : root.path("commits")) {
                String sha = text(commit, "id");
                String message = text(commit, "message");
                if (sha == null || message == null) {
                    continue;
                }
                String[] messageParts = message.split("\\R", 2);
                Map<String, Object> details = new LinkedHashMap<>();
                put(details, "beforeSha", text(root, "before"));
                put(details, "afterSha", text(root, "after"));
                details.put("created", root.path("created").asBoolean(false));
                details.put("deleted", root.path("deleted").asBoolean(false));
                details.put("forced", root.path("forced").asBoolean(false));
                details.put("distinct", commit.path("distinct").asBoolean(false));
                seeds.add(ActivitySeed.builder()
                        .externalKey("commit:" + branch + ":" + sha)
                        .type(ActivityType.PUSH_COMMIT)
                        .branchName(branch)
                        .commitSha(sha)
                        .title(limit(messageParts[0], 500))
                        .summary(messageParts.length == 2 ? limit(messageParts[1], 1000) : null)
                        .actorLogin(firstText(
                                commit.path("author"), "username", root.path("sender"), "login"
                        ))
                        .publicUrl(text(commit, "url"))
                        .occurredAt(parseTime(text(commit, "timestamp"), receivedAt))
                        .details(details)
                        .build());
            }
        }
        return new Result(seeds, seeds.isEmpty());
    }

    private Result pullRequest(
            Project project,
            String deliveryId,
            JsonNode root,
            OffsetDateTime receivedAt
    ) {
        String action = text(root, "action");
        JsonNode pr = root.path("pull_request");
        String baseRef = text(pr.path("base"), "ref");
        String headRef = text(pr.path("head"), "ref");
        if (!SUPPORTED_PR_ACTIONS.contains(action)
                || (!isTracked(project, baseRef) && !isTracked(project, headRef))) {
            return new Result(List.of(), true);
        }
        long number = pr.path("number").asLong(-1);
        if (number < 1) {
            return new Result(List.of(), true);
        }
        boolean merged = pr.path("merged").asBoolean(false);
        String occurredField = "opened".equals(action)
                ? "created_at"
                : "closed".equals(action) && merged
                ? "merged_at"
                : "closed".equals(action) ? "closed_at" : "updated_at";
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("prNumber", number);
        details.put("action", action);
        put(details, "state", text(pr, "state"));
        details.put("draft", pr.path("draft").asBoolean(false));
        details.put("merged", merged);
        put(details, "headRef", headRef);
        put(details, "headSha", text(pr.path("head"), "sha"));
        put(details, "baseRef", baseRef);
        put(details, "baseSha", text(pr.path("base"), "sha"));
        details.put("deliveryId", deliveryId);
        ActivitySeed seed = ActivitySeed.builder()
                .externalKey("pr:" + number + ":" + action + ":" + deliveryId)
                .type(ActivityType.PULL_REQUEST)
                .branchName(baseRef)
                .commitSha(text(pr.path("head"), "sha"))
                .title(limit(firstNonBlank(text(pr, "title"), "PR #" + number), 500))
                .summary("PR #" + number + " " + action + (merged ? " (merged)" : ""))
                .actorLogin(firstText(
                        root.path("sender"), "login", pr.path("user"), "login"
                ))
                .publicUrl(text(pr, "html_url"))
                .additions(integer(pr, "additions"))
                .deletions(integer(pr, "deletions"))
                .filesChanged(integer(pr, "changed_files"))
                .occurredAt(parseTime(text(pr, occurredField), receivedAt))
                .details(details)
                .build();
        return new Result(List.of(seed), false);
    }

    private boolean isTracked(Project project, String branch) {
        if (branch == null) {
            return false;
        }
        return project.getSettings().trackAllBranches()
                || branch.equals(project.getDefaultBranch())
                || project.getSettings().trackedBranches().contains(branch);
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isTextual() ? node.path(field).asText() : null;
    }

    private String firstText(JsonNode first, String firstField, JsonNode second, String secondField) {
        String value = text(first, firstField);
        return value == null ? text(second, secondField) : value;
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
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

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    public record Result(List<ActivitySeed> seeds, boolean ignored) {
        public Result {
            seeds = List.copyOf(seeds);
        }
    }
}
