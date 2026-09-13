package seungyong.helpmebackend.portfolio.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioNotionPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPdfPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;
import seungyong.helpmebackend.portfolio.application.port.out.RenderedPdf;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioExportWorkerTest {
    @Mock private PortfolioExportPortOut exportPortOut;
    @Mock private PortfolioPdfPortOut pdfPortOut;
    @Mock private PortfolioStoragePortOut storagePortOut;
    @Mock private PortfolioNotionPortOut notionPortOut;
    private PortfolioExportWorker worker;

    @BeforeEach
    void setUp() {
        worker = new PortfolioExportWorker(exportPortOut, pdfPortOut, storagePortOut, notionPortOut);
        ReflectionTestUtils.setField(worker, "pdfRetentionDays", 7);
    }

    @Test
    @DisplayName("PDF worker는 claim된 snapshot을 렌더링·업로드하고 성공 정보 저장")
    void runOnce_pdfSuccess() {
        PortfolioExport export = export();
        given(exportPortOut.claimNext(any(), any())).willReturn(Optional.of(export));
        given(pdfPortOut.render(export.document(), export.options())).willReturn(new RenderedPdf(new byte[]{1, 2}, 2));

        worker.runOnce();

        verify(storagePortOut).upload("portfolios/501/701.pdf", new byte[]{1, 2}, "application/pdf");
        verify(exportPortOut).completePdf(eq(701L), eq("portfolios/501/701.pdf"),
                eq("백엔드 포트폴리오.pdf"), eq(2L), eq(2), any(), any());
    }

    private PortfolioExport export() {
        PortfolioExportDocument document = new PortfolioExportDocument(1, "백엔드 포트폴리오",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), PortfolioDocument.empty(), List.of());
        return PortfolioExport.builder().id(701L).portfolioId(501L).projectId(101L)
                .format(PortfolioExportFormat.PDF).status(PortfolioExportStatus.PROCESSING)
                .document(document).options(PortfolioExportOptions.defaults(PortfolioExportFormat.PDF)).build();
    }
}
