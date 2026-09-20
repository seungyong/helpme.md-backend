package seungyong.helpmebackend.portfolio.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.portfolio.application.port.out.*;
import seungyong.helpmebackend.portfolio.domain.entity.*;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.project.application.port.out.ProjectWorkPortOut;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PortfolioExportPublisherTest {
    @Mock private ProjectWorkPortOut projects;
    @Mock private PortfolioExportPortOut exports;
    @Mock private PortfolioStoragePortOut storage;
    @Mock private PortfolioNotionPortOut notion;
    private PortfolioExportPublisher publisher;

    @BeforeEach
    void setup() {
        publisher = new PortfolioExportPublisher(projects, exports, storage, notion);
    }

    @Test
    @DisplayName("렌더링 중 프로젝트가 삭제되면 PDF 업로드를 시작하지 않음")
    void deletionDuringRenderPreventsUpload() {
        given(projects.lockActive(101L)).willReturn(false);
        publisher.publishPdf(export((short) 1), new RenderedPdf(new byte[]{1}, 1));
        verifyNoInteractions(storage, exports);
    }

    @Test
    @DisplayName("삭제된 프로젝트의 늦은 Notion 작업은 외부 페이지를 만들지 않음")
    void deletedProjectPreventsNotionWrite() {
        given(projects.lockActive(101L)).willReturn(false);
        publisher.publishNotion(export((short) 1));
        verifyNoInteractions(notion, exports);
    }

    @Test
    @DisplayName("stuck 복구 후 이전 시도의 늦은 렌더 결과는 업로드하지 않음")
    void staleAttemptCannotUpload() {
        given(projects.lockActive(101L)).willReturn(true);
        given(exports.getByPortfolioIdAndId(501L, 701L)).willReturn(Optional.of(export((short) 2)));
        publisher.publishPdf(export((short) 1), new RenderedPdf(new byte[]{1}, 1));
        verifyNoInteractions(storage);
    }

    private PortfolioExport export(short attempts) {
        var document = new PortfolioExportDocument(1, "테스트", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7), PortfolioDocument.empty(), List.of());
        return PortfolioExport.builder().id(701L).portfolioId(501L).projectId(101L)
                .format(PortfolioExportFormat.PDF).status(PortfolioExportStatus.PROCESSING)
                .document(document).options(PortfolioExportOptions.defaults(PortfolioExportFormat.PDF))
                .attempts(attempts).build();
    }
}
