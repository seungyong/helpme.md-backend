package seungyong.helpmebackend.webhook.adapter.out.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GithubInitialSyncAdapterTest {
    @Mock private GithubApiExecutor githubApiExecutor;
    private GithubInitialSyncAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GithubInitialSyncAdapter(githubApiExecutor, new ObjectMapper());
    }

    @Test
    @DisplayName("Events pagination만 사용해 최근 commit을 저장하고 기간 밖 이벤트에서 중단")
    void fetchActivities_success_boundedEventsPagination() {
        List<String> requestedUrls = new ArrayList<>();
        given(githubApiExecutor.executeGetJson(
                anyLong(), anyString(), anyString(), anyString(), any(), anyString()
        )).willAnswer(invocation -> {
            String url = invocation.getArgument(1);
            requestedUrls.add(url);
            if (url.contains("page=2")) {
                return handle(invocation, ResponseEntity.ok("""
                        [{
                          "id":"event-old",
                          "type":"PushEvent",
                          "created_at":"2026-06-01T00:00:00Z",
                          "actor":{"login":"seungyong"},
                          "payload":{"ref":"refs/heads/main","commits":[
                            {"sha":"old","message":"old"}
                          ]}
                        }]
                        """));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(
                    HttpHeaders.LINK,
                    "<https://api.github.com/repos/seungyong/helpme.md/events?per_page=100&page=2>; rel=\"next\""
            );
            return handle(invocation, new ResponseEntity<>("""
                    [{
                      "id":"event-new",
                      "type":"PushEvent",
                      "created_at":"2026-08-16T10:00:00Z",
                      "actor":{"login":"seungyong"},
                      "payload":{
                        "ref":"refs/heads/main",
                        "before":"before-sha",
                        "head":"after-sha",
                        "commits":[
                          {"sha":"sha-1","message":"첫 커밋"},
                          {"sha":"sha-2","message":"두 번째 커밋\\n상세"}
                        ]
                      }
                    }]
                    """, headers, HttpStatus.OK));
        });

        List<?> activities = adapter.fetchActivities(
                Project.builder()
                        .id(10L)
                        .userId(1L)
                        .repoFullName("seungyong/helpme.md")
                        .defaultBranch("main")
                        .build(),
                "github-token",
                OffsetDateTime.parse("2026-07-18T00:00:00Z"),
                3
        );

        assertThat(activities).hasSize(2);
        assertThat(requestedUrls).containsExactly(
                "https://api.github.com/repos/seungyong/helpme.md/events?per_page=100",
                "https://api.github.com/repos/seungyong/helpme.md/events?per_page=100&page=2"
        );
        assertThat(requestedUrls).noneMatch(url ->
                url.contains("/commits") || url.contains("/branches") || url.contains("/pulls")
        );
    }

    private Object handle(
            org.mockito.invocation.InvocationOnMock invocation,
            ResponseEntity<String> response
    ) {
        Function<ResponseEntity<String>, ?> handler = invocation.getArgument(4);
        return handler.apply(response);
    }
}
