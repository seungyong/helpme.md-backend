package seungyong.helpmebackend.portfolio.adapter.out.notion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.notion.application.port.out.NotionProviderPortOut;
import seungyong.helpmebackend.notion.application.port.out.exception.NotionProviderException;
import seungyong.helpmebackend.notion.application.port.out.result.NotionRefreshedTokens;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.notion.domain.type.NotionConnectionStatus;
import seungyong.helpmebackend.portfolio.application.port.out.NotionPageWriteResult;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioNotionProviderPortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioNotionAdapterTest {
    @Mock private NotionConnectionPortOut connectionPortOut;
    @Mock private NotionProviderPortOut notionProviderPortOut;
    @Mock private PortfolioNotionProviderPortOut exportProviderPortOut;
    @Mock private CipherPortOut cipherPortOut;

    @Test
    @DisplayName("access token 만료 시 한 번 갱신하고 같은 snapshot으로 Notion 내보내기 재시도")
    void export_refreshesExpiredToken() {
        PortfolioNotionAdapter adapter = new PortfolioNotionAdapter(
                connectionPortOut, notionProviderPortOut, exportProviderPortOut, cipherPortOut);
        PortfolioExportDocument document = new PortfolioExportDocument(1, "포트폴리오",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                PortfolioDocument.empty(), List.of());
        NotionConnection connection = NotionConnection.builder().id(11L).userId(1L)
                .status(NotionConnectionStatus.CONNECTED).encryptedAccessToken("encrypted-access")
                .encryptedRefreshToken("encrypted-refresh").build();
        given(connectionPortOut.getById(11L)).willReturn(Optional.of(connection));
        given(cipherPortOut.decrypt("encrypted-access")).willReturn("expired-access");
        given(cipherPortOut.decrypt("encrypted-refresh")).willReturn("refresh");
        given(cipherPortOut.encrypt("new-access")).willReturn("new-access-encrypted");
        given(cipherPortOut.encrypt("new-refresh")).willReturn("new-refresh-encrypted");
        given(notionProviderPortOut.refreshAccessToken("refresh"))
                .willReturn(new NotionRefreshedTokens("new-access", "new-refresh"));
        given(exportProviderPortOut.write(eq("expired-access"), eq("parent"), eq(document),
                eq(true), any(), any()))
                .willThrow(new NotionProviderException(NotionProviderException.Failure.UNAUTHORIZED));
        given(exportProviderPortOut.write(eq("new-access"), eq("parent"), eq(document),
                eq(true), any(), any()))
                .willReturn(new NotionPageWriteResult(false, "page-1", "포트폴리오", "https://notion.so/page-1"));

        var result = adapter.export(11L, "parent", document, true, null, null);

        assertThat(result.pageId()).isEqualTo("page-1");
        verify(connectionPortOut).rotateTokens(eq(1L), eq("new-access-encrypted"),
                eq("new-refresh-encrypted"), any());
    }
}
