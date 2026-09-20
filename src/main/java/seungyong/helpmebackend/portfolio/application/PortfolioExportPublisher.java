package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.portfolio.application.port.out.NotionExportResult;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioNotionPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.RenderedPdf;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioPdfStoragePath;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.project.application.port.out.ProjectWorkPortOut;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PortfolioExportPublisher {
    private final ProjectWorkPortOut projectWorkPortOut;
    private final PortfolioExportPortOut exportPortOut;
    private final PortfolioStoragePortOut storagePortOut;
    private final PortfolioNotionPortOut notionPortOut;

    @Value("${portfolio.export.pdf-retention-days:7}")
    private int pdfRetentionDays;

    @Transactional
    public void publishPdf(PortfolioExport export, RenderedPdf pdf) {
        // 렌더링은 transaction 밖에서 수행하고 업로드·결과 저장만 프로젝트 삭제와 직렬화
        if (!lockCurrent(export)) return;
        String path = PortfolioPdfStoragePath.of(export.portfolioId(), export.id());
        storagePortOut.upload(path, pdf.bytes(), "application/pdf");
        OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String name = export.document().title().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        exportPortOut.completePdf(export.id(), path, (name.isBlank() ? "portfolio" : name) + ".pdf",
                pdf.bytes().length, pdf.pageCount(), completedAt.plusDays(pdfRetentionDays), completedAt);
    }

    @Transactional
    public void publishNotion(PortfolioExport export) {
        // 삭제 시작 이후 새 Notion 페이지 작성 및 토큰 정리와의 경합 차단
        if (!lockCurrent(export)) return;
        NotionExportResult result = notionPortOut.export(export.notionConnectionId(),
                export.notionParentPageId(), export.document(), export.options().checkTitleConflict(),
                export.conflictAction(), export.conflictPageId());
        if (result.needsAction()) {
            exportPortOut.requireConflictAction(export.id(), result.pageId(), result.pageTitle(), result.pageUrl());
        } else {
            exportPortOut.completeNotion(export.id(), result.pageId(), result.pageUrl(),
                    OffsetDateTime.now(ZoneOffset.UTC));
        }
    }

    private boolean lockCurrent(PortfolioExport export) {
        if (!projectWorkPortOut.lockActive(export.projectId())) return false;
        return exportPortOut.getByPortfolioIdAndId(export.portfolioId(), export.id())
                .filter(current -> current.status() == PortfolioExportStatus.PROCESSING)
                .filter(current -> current.attempts() == export.attempts()
                        && Objects.equals(current.startedAt(), export.startedAt()))
                .isPresent();
    }
}
