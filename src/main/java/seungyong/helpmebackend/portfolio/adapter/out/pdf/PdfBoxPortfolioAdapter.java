package seungyong.helpmebackend.portfolio.adapter.out.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPdfPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.RenderedPdf;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportErrorCode;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportProcessingException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfBoxPortfolioAdapter implements PortfolioPdfPortOut {
    private static final float MARGIN = 54;
    private static final float BODY_SIZE = 10.5f;
    private static final float LINE_HEIGHT = 16;

    @Value("${portfolio.export.pdf.font-path:}")
    private String configuredFontPath;

    @Override
    public RenderedPdf render(PortfolioExportDocument document, PortfolioExportOptions options) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont font = PDType0Font.load(pdf, resolveFont());
            Writer writer = new Writer(pdf, font, options.includePageNumbers());
            if (options.includeCoverAndToc()) writeCover(writer, document, options.showGeneratedAt());
            for (PortfolioDocument.Section section : document.content().sections()) {
                writer.heading(section.title());
                writer.paragraph(section.contentMd());
            }
            if (options.includeEvidenceLinks() && !document.evidenceLinks().isEmpty()) {
                writer.heading("근거 링크");
                for (PortfolioExportDocument.EvidenceLink link : document.evidenceLinks()) {
                    writer.paragraph("- " + link.label() + ": " + link.url());
                }
            }
            writer.finish();
            pdf.save(output);
            return new RenderedPdf(output.toByteArray(), pdf.getNumberOfPages());
        } catch (IOException | RuntimeException exception) {
            throw new PortfolioExportProcessingException(
                    PortfolioExportErrorCode.PDF_RENDER_FAILED.getErrorCode(),
                    PortfolioExportErrorCode.PDF_RENDER_FAILED.getMessage()
            );
        }
    }

    private void writeCover(Writer writer, PortfolioExportDocument document, boolean showGeneratedAt)
            throws IOException {
        writer.title(document.title());
        writer.paragraph(document.periodStart() + " - " + document.periodEnd());
        if (showGeneratedAt) writer.paragraph("생성 시각: " + OffsetDateTime.now(ZoneOffset.UTC));
        writer.heading("목차");
        int number = 1;
        for (PortfolioDocument.Section section : document.content().sections()) {
            writer.paragraph(number++ + ". " + section.title());
        }
        writer.pageBreak();
    }

    private File resolveFont() {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(configuredFontPath)) candidates.add(configuredFontPath);
        candidates.add("C:/Windows/Fonts/malgun.ttf");
        candidates.add("/usr/share/fonts/truetype/nanum/NanumGothic.ttf");
        return candidates.stream().map(File::new).filter(File::isFile).findFirst()
                .orElseThrow(() -> new IllegalStateException("Korean PDF font not found"));
    }

    private static final class Writer {
        private final PDDocument document;
        private final PDFont font;
        private final boolean pageNumbers;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private Writer(PDDocument document, PDFont font, boolean pageNumbers) throws IOException {
            this.document = document;
            this.font = font;
            this.pageNumbers = pageNumbers;
            newPage();
        }

        private void title(String text) throws IOException {
            lines(text, 24, 31);
            y -= 16;
        }

        private void heading(String text) throws IOException {
            ensureSpace(34);
            y -= 8;
            lines(text, 15, 23);
            y -= 5;
        }

        private void paragraph(String markdown) throws IOException {
            String normalized = markdown == null ? "" : markdown
                    .replace("**", "").replace("__", "").replace("`", "");
            for (String paragraph : normalized.split("\\R", -1)) {
                if (paragraph.isBlank()) {
                    y -= LINE_HEIGHT / 2;
                } else {
                    lines(paragraph, BODY_SIZE, LINE_HEIGHT);
                }
            }
            y -= 4;
        }

        private void lines(String text, float size, float lineHeight) throws IOException {
            float available = PDRectangle.A4.getWidth() - MARGIN * 2;
            StringBuilder line = new StringBuilder();
            for (String token : text.split("(?<=\\s)|(?=\\s)|(?<=[가-힣])|(?=[가-힣])")) {
                String candidate = line + token;
                if (!line.isEmpty() && width(candidate, size) > available) {
                    writeLine(line.toString().stripTrailing(), size, lineHeight);
                    line.setLength(0);
                }
                line.append(token);
            }
            if (!line.isEmpty()) writeLine(line.toString().stripTrailing(), size, lineHeight);
        }

        private float width(String text, float size) throws IOException {
            return font.getStringWidth(text) / 1000 * size;
        }

        private void writeLine(String text, float size, float lineHeight) throws IOException {
            ensureSpace(lineHeight);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(text);
            stream.endText();
            y -= lineHeight;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < MARGIN) newPage();
        }

        private void pageBreak() throws IOException {
            newPage();
        }

        private void newPage() throws IOException {
            closePage();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void closePage() throws IOException {
            if (stream == null) return;
            if (pageNumbers) {
                stream.beginText();
                stream.setFont(font, 8);
                stream.newLineAtOffset(page.getMediaBox().getWidth() / 2 - 3, 28);
                stream.showText(String.valueOf(document.getNumberOfPages()));
                stream.endText();
            }
            stream.close();
        }

        private void finish() throws IOException {
            closePage();
            stream = null;
        }
    }
}
