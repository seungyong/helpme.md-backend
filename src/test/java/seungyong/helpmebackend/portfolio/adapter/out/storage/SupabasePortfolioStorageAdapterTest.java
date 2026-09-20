package seungyong.helpmebackend.portfolio.adapter.out.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportProcessingException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SupabasePortfolioStorageAdapterTest {
    private MockRestServiceServer server;
    private SupabasePortfolioStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new SupabasePortfolioStorageAdapter(builder);
        ReflectionTestUtils.setField(adapter, "baseUrl", "https://supabase.test/storage/v1");
        ReflectionTestUtils.setField(adapter, "serviceRoleKey", "test-service-role-key");
        ReflectionTestUtils.setField(adapter, "bucket", "portfolio-exports");
    }

    @Test
    @DisplayName("이미 없는 PDF 객체의 404는 멱등 삭제 성공으로 처리")
    void deleteTreatsNotFoundAsSuccess() {
        server.expect(once(), requestTo(
                        "https://supabase.test/storage/v1/object/portfolio-exports/portfolios%2F501%2F601.pdf"
                )).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatCode(() -> adapter.delete("portfolios/501/601.pdf"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("Storage 5xx는 프로젝트 삭제를 보류할 수 있도록 실패로 전달")
    void deletePropagatesUpstreamFailure() {
        server.expect(once(), requestTo(
                        "https://supabase.test/storage/v1/object/portfolio-exports/portfolios%2F501%2F601.pdf"
                )).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.delete("portfolios/501/601.pdf"))
                .isInstanceOf(PortfolioExportProcessingException.class);
        server.verify();
    }
}
