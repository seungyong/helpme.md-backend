package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.notion.domain.exception.NotionErrorCode;

import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;

import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPdfPortOut;

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
    private final PortfolioExportPublisher publisher;



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

        publisher.publishPdf(export, pdf);
    }

    private void processNotion(PortfolioExport export) {
        publisher.publishNotion(export);
    }


    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
