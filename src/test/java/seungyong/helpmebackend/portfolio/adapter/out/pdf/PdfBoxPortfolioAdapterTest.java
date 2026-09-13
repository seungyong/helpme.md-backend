package seungyong.helpmebackend.portfolio.adapter.out.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfBoxPortfolioAdapterTest {
    @Test
    @DisplayName("한국어 포트폴리오를 읽을 수 있는 PDF로 생성")
    void render_koreanDocument() throws Exception {
        PdfBoxPortfolioAdapter adapter = new PdfBoxPortfolioAdapter();
        ReflectionTestUtils.setField(adapter, "configuredFontPath", "C:/Windows/Fonts/malgun.ttf");
        PortfolioExportDocument document = new PortfolioExportDocument(1, "도움 프로젝트",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new PortfolioDocument(1, List.of(new PortfolioDocument.Section(
                        "summary", "summary", "핵심 성과", "로그인과 웹훅을 구현함", List.of()))),
                List.of(new PortfolioExportDocument.EvidenceLink("activity:1", "구현 커밋", "https://example.com")));

        var rendered = adapter.render(document, new PortfolioExportOptions(true, true, true, false, false));
        Path sample = Path.of("build", "reports", "portfolio-export-sample.pdf");
        Files.createDirectories(sample.getParent());
        Files.write(sample, rendered.bytes());

        assertThat(rendered.bytes()).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        try (PDDocument pdf = Loader.loadPDF(rendered.bytes())) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("도움 프로젝트", "핵심 성과", "구현 커밋");
            assertThat(pdf.getNumberOfPages()).isEqualTo(rendered.pageCount());
        }
    }
}
