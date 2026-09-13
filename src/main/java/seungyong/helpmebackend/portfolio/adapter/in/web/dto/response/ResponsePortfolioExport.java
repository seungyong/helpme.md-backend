package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResponsePortfolioExport(
        Long id, String format, String status, int portfolioVersion,
        DocumentSummary documentSummary, short attempts,
        OffsetDateTime createdAt, OffsetDateTime startedAt, OffsetDateTime completedAt,
        File file, NotionPage notionPage, Conflict conflict, Error error
) {
    public static ResponsePortfolioExport from(PortfolioExport export, OffsetDateTime now) {
        String status = export.effectiveStatus(now).getDatabaseValue();
        File file = export.fileName() == null ? null : new File(
                export.fileName(), export.fileSizeBytes(), export.pageCount(), export.expiresAt(),
                export.status() == seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus.SUCCEEDED
                        && !export.isExpired(now));
        NotionPage notionPage = export.notionPageId() == null ? null
                : new NotionPage(export.notionPageId(), export.document().title(), export.notionPageUrl());
        Conflict conflict = export.conflictPageId() == null ? null
                : new Conflict(export.conflictPageId(), export.conflictPageTitle(), export.conflictPageUrl(),
                List.of("update", "copy"));
        Error error = export.errorCode() == null ? null : new Error(export.errorCode(), export.errorMessage(), true);
        return new ResponsePortfolioExport(export.id(), export.format().getDatabaseValue(), status,
                export.portfolioVersion(), new DocumentSummary(export.document().title(),
                export.document().periodStart(), export.document().periodEnd(),
                export.document().content().sections().size()), export.attempts(), export.createdAt(),
                export.startedAt(), export.completedAt(), file, notionPage, conflict, error);
    }

    public record DocumentSummary(String title, LocalDate periodStart, LocalDate periodEnd, int sectionCount) {
    }
    public record File(String fileName, Long fileSizeBytes, Integer pageCount, OffsetDateTime expiresAt,
                       boolean downloadAvailable) {
    }
    public record NotionPage(String pageId, String title, String url) {
    }
    public record Conflict(String existingPageId, String title, String url, List<String> allowedActions) {
    }
    public record Error(String code, String message, boolean retryable) {
    }
}
