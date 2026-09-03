package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectList;

import java.time.OffsetDateTime;
import java.util.List;

public record ResponseProjects(
        Plan plan,
        List<Item> items,
        Page page
) {
    public static ResponseProjects from(ProjectList result) {
        return new ResponseProjects(
                new Plan(result.plan().code(), result.plan().limit(), result.plan().used()),
                result.items().stream().map(Item::from).toList(),
                new Page(result.page().nextCursor(), result.page().hasNext())
        );
    }

    public record Plan(String code, int limit, long used) {
    }

    public record Item(
            Long id,
            String repoFullname,
            String defaultBranch,
            @JsonProperty("isPrivate") boolean privateRepository,
            String status,
            String syncStatus,
            String webhookStatus,
            OffsetDateTime webhookLastReceivedAt,
            String timezone,
            @JsonProperty("isLocked") boolean locked,
            boolean attentionRequired,
            Metrics metrics
    ) {
        private static Item from(ProjectList.Item item) {
            Project project = item.project();
            return new Item(
                    project.getId(),
                    project.getRepoFullName(),
                    project.getDefaultBranch(),
                    project.isPrivateRepository(),
                    project.getStatus().getDatabaseValue(),
                    project.getSync().status().getDatabaseValue(),
                    project.getWebhook().status().getDatabaseValue(),
                    project.getWebhook().lastReceivedAt(),
                    project.getSettings().timezone(),
                    item.locked(),
                    item.attentionRequired(),
                    new Metrics(
                            item.metrics().eventCount7d(),
                            item.metrics().completedReflectionCount(),
                            item.metrics().lastActivityTitle()
                    )
            );
        }
    }

    public record Metrics(
            long eventCount7d,
            long completedReflectionCount,
            String lastActivityTitle
    ) {
    }

    public record Page(String nextCursor, boolean hasNext) {
    }
}
