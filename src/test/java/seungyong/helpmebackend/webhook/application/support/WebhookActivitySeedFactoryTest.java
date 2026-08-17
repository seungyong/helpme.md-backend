package seungyong.helpmebackend.webhook.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookActivitySeedFactoryTest {
    private final WebhookActivitySeedFactory factory = new WebhookActivitySeedFactory(
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void createsCommitKeyPerBranchAndSha() {
        WebhookActivitySeedFactory.Result result = factory.create(
                project(), "push", "delivery-1",
                Map.of(
                        "ref", "refs/heads/main",
                        "commits", List.of(Map.of(
                                "id", "abc123", "message", "feat: activity\nsummary",
                                "timestamp", "2026-08-17T00:00:00Z",
                                "author", Map.of("username", "octocat")
                        )),
                        "sender", Map.of("login", "octocat")
                ),
                OffsetDateTime.parse("2026-08-17T00:01:00Z")
        );

        assertThat(result.ignored()).isFalse();
        assertThat(result.seeds()).singleElement().satisfies(seed -> {
            assertThat(seed.externalKey()).isEqualTo("commit:main:abc123");
            assertThat(seed.title()).isEqualTo("feat: activity");
            assertThat(seed.summary()).isEqualTo("summary");
        });
    }

    @Test
    void ignoresUntrackedBranch() {
        WebhookActivitySeedFactory.Result result = factory.create(
                project(), "push", "delivery-1",
                Map.of("ref", "refs/heads/feature/ignored", "commits", List.of()),
                OffsetDateTime.parse("2026-08-17T00:01:00Z")
        );

        assertThat(result.ignored()).isTrue();
        assertThat(result.seeds()).isEmpty();
    }

    private Project project() {
        return Project.builder()
                .id(101L)
                .userId(1L)
                .repoFullName("octocat/demo")
                .githubRepoId(778899L)
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .settings(new ProjectSettings(
                        List.of("main", "develop"), false, "Asia/Seoul",
                        ProjectSettings.defaults().daily(), ProjectSettings.defaults().weekly(),
                        (short) 30
                ))
                .build();
    }
}
