package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.notion.domain.exception.NotionErrorCode;
import seungyong.helpmebackend.portfolio.application.port.out.NotionExportResult;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioNotionPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPdfPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;
import seungyong.helpmebackend.portfolio.application.port.out.RenderedPdf;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportErrorCode;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportProcessingException;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioExportWorker {
    private static final Duration STUCK_AFTER = Duration.ofMinutes(5);

    private final PortfolioExportPortOut exportPortOut;
    private final PortfolioPdfPortOut pdfPortOut;
    private final PortfolioStoragePortOut storagePortOut;
    private final PortfolioNotionPortOut notionPortOut;

    @Value("${portfolio.export.pdf-retention-days:7}")
    private int pdfRetentionDays;

    @Scheduled(fixedDelayString = "${workers.portfolio-export.fixed-delay-ms:1000}")
    public void runOnce() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        exportPortOut.claimNext(now, now.minus(STUCK_AFTER)).ifPresent(this::process);
    }

    private void process(PortfolioExport export) {
        try {
            if (export.format() == PortfolioExportFormat.PDF) processPdf(export);
            else processNotion(export);
        } catch (PortfolioExportProcessingException exception) {
            exportPortOut.fail(export.id(), exception.getErrorCode(), exception.getMessage(), now());
        } catch (RuntimeException exception) {
            String code = export.format() == PortfolioExportFormat.PDF
                    ? PortfolioExportErrorCode.PDF_RENDER_FAILED.getErrorCode()
                    : NotionErrorCode.NOTION_UPSTREAM_ERROR.getErrorCode();
            String message = export.format() == PortfolioExportFormat.PDF
                    ? PortfolioExportErrorCode.PDF_RENDER_FAILED.getMessage()
                    : NotionErrorCode.NOTION_UPSTREAM_ERROR.getMessage();
            exportPortOut.fail(export.id(), code, message, now());
            log.warn("Portfolio export failed: exportId={}, type={}", export.id(), exception.getClass().getSimpleName());
        }
    }

    private void processPdf(PortfolioExport export) {
        RenderedPdf pdf = pdfPortOut.render(export.document(), export.options());
        String fileName = safeFileName(export.document().title()) + ".pdf";
        String path = "portfolios/" + export.portfolioId() + "/" + export.id() + ".pdf";
        storagePortOut.upload(path, pdf.bytes(), "application/pdf");
        OffsetDateTime completedAt = now();
        exportPortOut.completePdf(export.id(), path, fileName, pdf.bytes().length, pdf.pageCount(),
                completedAt.plusDays(pdfRetentionDays), completedAt);
    }

    private void processNotion(PortfolioExport export) {
        NotionExportResult result = notionPortOut.export(export.notionConnectionId(),
                export.notionParentPageId(), export.document(), export.options().checkTitleConflict(),
                export.conflictAction(), export.conflictPageId());
        if (result.needsAction()) {
            exportPortOut.requireConflictAction(export.id(), result.pageId(), result.pageTitle(),
                    result.pageUrl());
        } else {
            exportPortOut.completeNotion(export.id(), result.pageId(), result.pageUrl(), now());
        }
    }

    private String safeFileName(String title) {
        String sanitized = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isBlank() ? "portfolio" : sanitized;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
