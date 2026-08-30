package seungyong.helpmebackend.notion.adapter.out.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import seungyong.helpmebackend.notion.application.port.out.exception.NotionProviderException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

class NotionApiAdapterTest {
    private MockRestServiceServer server;
    private NotionApiAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new NotionApiAdapter(builder);
        ReflectionTestUtils.setField(adapter, "clientId", "test-client-id");
        ReflectionTestUtils.setField(adapter, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(adapter, "redirectUri", "https://backend.example/api/v1/integrations/notion/oauth/callback");
        ReflectionTestUtils.setField(adapter, "apiBaseUrl", "https://api.notion.test");
        ReflectionTestUtils.setField(adapter, "apiVersion", "2026-03-11");
    }

    @Test
    @DisplayName("페이지 검색은 query·cursor·size와 Notion-Version을 한 요청에 전달")
    void searchPages_success() {
        server.expect(once(), requestTo("https://api.notion.test/v1/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(header("Notion-Version", "2026-03-11"))
                .andRespond(withSuccess("""
                        {
                          "results":[{
                            "id":"page-1",
                            "last_edited_time":"2026-08-23T10:00:00Z",
                            "properties":{"Name":{"type":"title","title":[{"plain_text":"Portfolio"}]}}
                          }],
                          "next_cursor":"cursor-2",
                          "has_more":true
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.searchPages(
                "access-token", "portfolio", "cursor-1", 20
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Portfolio");
        assertThat(result.nextCursor()).isEqualTo("cursor-2");
        assertThat(result.hasNext()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("페이지 수정 시각이 누락되면 예외 없이 null로 변환")
    void retrievePage_withoutLastEditedTime() {
        server.expect(once(), requestTo("https://api.notion.test/v1/pages/page-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id":"page-1",
                          "properties":{
                            "Name":{
                              "type":"title",
                              "title":[{"plain_text":"Portfolio"}]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.retrievePage("access-token", "page-1");

        assertThat(result.id()).isEqualTo("page-1");
        assertThat(result.title()).isEqualTo("Portfolio");
        assertThat(result.lastEditedAt()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("Notion 429는 Retry-After를 보존한 provider rate-limit 오류로 변환")
    void searchPages_rateLimit() {
        server.expect(once(), requestTo("https://api.notion.test/v1/search"))
                .andRespond(withTooManyRequests().header(HttpHeaders.RETRY_AFTER, "7"));

        assertThatThrownBy(() -> adapter.searchPages("token", null, null, 30))
                .isInstanceOfSatisfying(NotionProviderException.class, error -> {
                    assertThat(error.getFailure())
                            .isEqualTo(NotionProviderException.Failure.RATE_LIMIT);
                    assertThat(error.getRetryAfterSeconds()).isEqualTo(7);
                });
        server.verify();
    }

    @Test
    @DisplayName("OAuth URL은 @Value 설정과 state를 인코딩해 생성")
    void buildAuthorizationUrl_success() {
        String url = adapter.buildAuthorizationUrl("state value");

        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("state=state%20value");
        assertThat(url).contains("response_type=code");
    }
}
