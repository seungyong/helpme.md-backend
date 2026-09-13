package seungyong.helpmebackend.portfolio.adapter.out.notion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.notion.application.port.out.NotionProviderPortOut;
import seungyong.helpmebackend.notion.application.port.out.exception.NotionProviderException;
import seungyong.helpmebackend.notion.application.port.out.result.NotionRefreshedTokens;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.notion.domain.exception.NotionErrorCode;
import seungyong.helpmebackend.portfolio.application.port.out.NotionExportResult;
import seungyong.helpmebackend.portfolio.application.port.out.NotionPageWriteResult;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioNotionPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioNotionProviderPortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportProcessingException;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class PortfolioNotionAdapter implements PortfolioNotionPortOut {
    private final NotionConnectionPortOut connectionPortOut;
    private final NotionProviderPortOut notionProviderPortOut;
    private final PortfolioNotionProviderPortOut exportProviderPortOut;
    private final CipherPortOut cipherPortOut;

    @Override
    public NotionExportResult export(Long notionConnectionId, String parentPageId,
                                     PortfolioExportDocument document, boolean checkTitleConflict,
                                     PortfolioConflictAction conflictAction, String conflictPageId) {
        NotionConnection connection = connectionPortOut.getById(notionConnectionId)
                .filter(NotionConnection::isConnected).filter(NotionConnection::hasTokenPair)
                .orElseThrow(() -> failure(NotionErrorCode.NOTION_CONNECTION_EXPIRED));
        try {
            return toResult(exportProviderPortOut.write(
                    cipherPortOut.decrypt(connection.getEncryptedAccessToken()), parentPageId,
                    document, checkTitleConflict, conflictAction, conflictPageId));
        } catch (NotionProviderException first) {
            if (first.getFailure() != NotionProviderException.Failure.UNAUTHORIZED) throw mapped(first);
        }

        // 만료된 access token은 한 번만 갱신하고 동일한 고정 snapshot을 다시 전송
        try {
            NotionRefreshedTokens refreshed = notionProviderPortOut.refreshAccessToken(
                    cipherPortOut.decrypt(connection.getEncryptedRefreshToken()));
            connectionPortOut.rotateTokens(connection.getUserId(),
                    cipherPortOut.encrypt(refreshed.getAccessToken()),
                    cipherPortOut.encrypt(refreshed.getRefreshToken()), OffsetDateTime.now(ZoneOffset.UTC));
            return toResult(exportProviderPortOut.write(refreshed.getAccessToken(), parentPageId,
                    document, checkTitleConflict, conflictAction, conflictPageId));
        } catch (NotionProviderException exception) {
            if (exception.getFailure() == NotionProviderException.Failure.UNAUTHORIZED) {
                connectionPortOut.markReconnectRequired(connection.getUserId(),
                        NotionErrorCode.NOTION_CONNECTION_EXPIRED.getErrorCode(),
                        NotionErrorCode.NOTION_CONNECTION_EXPIRED.getMessage(), OffsetDateTime.now(ZoneOffset.UTC));
            }
            throw mapped(exception);
        }
    }

    private NotionExportResult toResult(NotionPageWriteResult result) {
        return result.conflict()
                ? NotionExportResult.conflict(result.pageId(), result.pageTitle(), result.pageUrl())
                : NotionExportResult.succeeded(result.pageId(), result.pageUrl());
    }

    private PortfolioExportProcessingException mapped(NotionProviderException exception) {
        return switch (exception.getFailure()) {
            case UNAUTHORIZED -> failure(NotionErrorCode.NOTION_CONNECTION_EXPIRED);
            case FORBIDDEN -> failure(NotionErrorCode.NOTION_PAGE_ACCESS_DENIED);
            case NOT_FOUND -> failure(NotionErrorCode.NOTION_PARENT_PAGE_NOT_FOUND);
            case BAD_REQUEST, RATE_LIMIT, UPSTREAM -> failure(NotionErrorCode.NOTION_UPSTREAM_ERROR);
        };
    }

    private PortfolioExportProcessingException failure(NotionErrorCode code) {
        return new PortfolioExportProcessingException(code.getErrorCode(), code.getMessage());
    }
}
