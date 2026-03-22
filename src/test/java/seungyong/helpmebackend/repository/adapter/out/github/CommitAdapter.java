package seungyong.helpmebackend.repository.adapter.out.github;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.result.CommitResult;
import seungyong.helpmebackend.repository.application.port.out.result.ContributorsResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommitAdapterTest {
    @Mock private GithubApiExecutor githubApiExecutor;

    @InjectMocks private CommitAdapter commitAdapter;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    private String createCommitJson(int count, String prefix) {
        List<String> commits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            commits.add(String.format(
                    "{\"sha\":\"%s-%d\",\"commit\":{\"message\":\"msg\",\"committer\":{\"date\":\"%s\"}}}",
                    prefix, i, Instant.now().toString()
            ));
        }
        return "[" + String.join(",", commits) + "]";
    }

    private HttpHeaders createLinkHeader(Integer lastPage) {
        HttpHeaders headers = new HttpHeaders();
        if (lastPage != null) {
            headers.set(HttpHeaders.LINK, "<https://api.github.com/repositories/123/commits?page=" + lastPage + ">; rel=\"last\"");
        }
        return headers;
    }

    private void mockApi(int page, int count, String prefix, HttpHeaders headers) {
        ResponseEntity<String> response = ResponseEntity.ok()
                .headers(headers == null ? new HttpHeaders() : headers)
                .body(createCommitJson(count, prefix));

        given(githubApiExecutor.executeGetJson(
                contains("&page=" + page), anyString(), any(), any(), anyString(), any()
        )).willReturn(response);
    }

    @Nested
    @DisplayName("getCommits - 커밋 목록 조회")
    class GetCommits {
        @Test
        @DisplayName("성공 - 페이지가 1개인 경우")
        void getCommits_success_page_1() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            ContributorsResult.Contributor contributor = fixtureMonkey.giveMeOne(ContributorsResult.Contributor.class);

            mockApi(1, 40, "p1", createLinkHeader(null));

            CommitResult result = commitAdapter.getCommits(command, contributor);

            assertThat(result.latestCommits()).hasSize(40);
            assertThat(result.middleCommits()).isEmpty();
            assertThat(result.initialCommits()).isEmpty();
        }

        @Test
        @DisplayName("성공 - 페이지가 2개인 경우 (middle이 1이므로 빈 리스트 반환)")
        void getCommits_success_page_2() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            ContributorsResult.Contributor contributor = fixtureMonkey.giveMeOne(ContributorsResult.Contributor.class);

            mockApi(1, 40, "p1", createLinkHeader(2));
            mockApi(2, 20, "p2", null);

            CommitResult result = commitAdapter.getCommits(command, contributor);

            assertThat(result.latestCommits()).hasSize(40);
            assertThat(result.middleCommits()).isEmpty();
            assertThat(result.initialCommits()).hasSize(20);
        }

        @Test
        @DisplayName("성공 - 페이지가 3개인 경우")
        void getCommits_success_page_3() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            ContributorsResult.Contributor contributor = fixtureMonkey.giveMeOne(ContributorsResult.Contributor.class);

            mockApi(1, 40, "p1", createLinkHeader(3));
            mockApi(2, 40, "p2", null);
            mockApi(3, 10, "p3", null);

            CommitResult result = commitAdapter.getCommits(command, contributor);

            assertThat(result.latestCommits()).hasSize(40);
            assertThat(result.middleCommits()).hasSize(40);
            assertThat(result.middleCommits().get(0).sha()).startsWith("p2");
            assertThat(result.initialCommits()).hasSize(10);
            assertThat(result.initialCommits().get(0).sha()).startsWith("p3");
        }

        @Test
        @DisplayName("성공 - 페이지가 4개이고 마지막 페이지가 40개인 경우")
        void getCommits_success_page_4_last_full() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            ContributorsResult.Contributor contributor = fixtureMonkey.giveMeOne(ContributorsResult.Contributor.class);

            mockApi(1, 40, "p1", createLinkHeader(4));
            mockApi(2, 40, "p2", null);
            mockApi(4, 40, "p4", null);

            CommitResult result = commitAdapter.getCommits(command, contributor);

            assertThat(result.latestCommits()).hasSize(40);
            assertThat(result.middleCommits()).hasSize(40);
            assertThat(result.initialCommits()).hasSize(40);
            assertThat(result.initialCommits().get(0).sha()).startsWith("p4");
        }

        @Test
        @DisplayName("성공 - 페이지가 4개이고 마지막 페이지가 40개 미만인 경우 (이전 페이지 호출하여 개수 채움)")
        void getCommits_success_page_4_last_under_40() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            ContributorsResult.Contributor contributor = fixtureMonkey.giveMeOne(ContributorsResult.Contributor.class);

            mockApi(1, 40, "p1", createLinkHeader(4));
            mockApi(2, 40, "p2", null);
            mockApi(3, 40, "p3", null);
            mockApi(4, 15, "p4", null);

            CommitResult result = commitAdapter.getCommits(command, contributor);

            assertThat(result.latestCommits()).hasSize(40);
            assertThat(result.middleCommits()).hasSize(40);
            assertThat(result.initialCommits()).hasSize(40);

            assertThat(result.initialCommits().get(24).sha()).startsWith("p3");
            assertThat(result.initialCommits().get(25).sha()).startsWith("p4");
        }

        @Test
        @DisplayName("성공 - 페이지가 5개이고 마지막 페이지가 40개 미만인 경우 (이전 페이지 호출하여 개수 채움)")
        void getCommits_success_page_5_last_under_40() {
            RepoBranchCommand command = fixtureMonkey.giveMeOne(RepoBranchCommand.class);
            ContributorsResult.Contributor contributor = fixtureMonkey.giveMeOne(ContributorsResult.Contributor.class);

            mockApi(1, 40, "p1", createLinkHeader(5));
            mockApi(3, 40, "p3", null);
            mockApi(4, 40, "p4", null);
            mockApi(5, 10, "p5", null);

            CommitResult result = commitAdapter.getCommits(command, contributor);

            assertThat(result.latestCommits()).hasSize(40);

            // 1 ~ 5페이지의 중간은 3으로 지정됨
            assertThat(result.middleCommits()).hasSize(40);
            assertThat(result.middleCommits().get(0).sha()).startsWith("p3");

            assertThat(result.initialCommits()).hasSize(40);
            assertThat(result.initialCommits().get(29).sha()).startsWith("p4");
            assertThat(result.initialCommits().get(30).sha()).startsWith("p5");
        }
    }
}