package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class PortfolioExportAdapterTest {
    @Autowired private PortfolioExportPortOut exportPortOut;
    @Autowired private PortfolioPortOut portfolioPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;

    @Test
    @DisplayName("PDF 내보내기의 claim, 실패, 동일 snapshot 재시도, 성공, 만료 상태를 DB에 보존")
    void pdfLifecycle() {
        User user = userPortOut.save(user(null, "export-token"));
        Project project = projectPortOut.save(project(user.getId()));
        Portfolio portfolio = portfolioPortOut.createIfAbsent(portfolio(project.getId())).portfolio();
        PortfolioExport created = exportPortOut.save(export(portfolio));
        // 아직 storagePath가 없는 export도 업로드 예정 경로로 정리 대상에 포함
        assertThat(exportPortOut.findPdfStoragePathsByProjectId(project.getId()))
                .containsExactly("portfolios/" + portfolio.id() + "/" + created.id() + ".pdf");

        PortfolioExport claimed = exportPortOut.claimNext(
                OffsetDateTime.parse("2026-09-06T00:00:00Z"),
                OffsetDateTime.parse("2026-09-05T23:55:00Z")).orElseThrow();
        assertThat(claimed.status()).isEqualTo(PortfolioExportStatus.PROCESSING);

        exportPortOut.fail(created.id(), "EXPORT_50001", "실패",
                OffsetDateTime.parse("2026-09-06T00:00:10Z"));
        PortfolioExport retried = exportPortOut.retry(created.id()).orElseThrow();
        assertThat(retried.status()).isEqualTo(PortfolioExportStatus.QUEUED);
        assertThat(retried.attempts()).isEqualTo((short) 2);
        assertThat(retried.document()).isEqualTo(created.document());

        exportPortOut.claimNext(OffsetDateTime.parse("2026-09-06T00:01:00Z"),
                OffsetDateTime.parse("2026-09-05T23:56:00Z")).orElseThrow();
        exportPortOut.completePdf(created.id(), "portfolios/1/1.pdf", "portfolio.pdf",
                1024, 2, OffsetDateTime.parse("2026-09-13T00:01:00Z"),
                OffsetDateTime.parse("2026-09-06T00:01:00Z"));

        assertThat(exportPortOut.findExpiredPdf(OffsetDateTime.parse("2026-09-13T00:02:00Z"), 10))
                .extracting(PortfolioExport::id).containsExactly(created.id());
        exportPortOut.markExpired(created.id());
        PortfolioExport expired = exportPortOut.getByPortfolioIdAndId(portfolio.id(), created.id()).orElseThrow();
        assertThat(expired.status()).isEqualTo(PortfolioExportStatus.EXPIRED);
        assertThat(expired.storagePath()).isNull();
    }

    private Portfolio portfolio(Long projectId) {
        return Portfolio.builder().projectId(projectId).requestKey(UUID.randomUUID()).title("포트폴리오")
                .periodStart(LocalDate.of(2026, 8, 1)).periodEnd(LocalDate.of(2026, 8, 31))
                .tone(PortfolioTone.CONCISE).status(PortfolioStatus.SAVED)
                .content(PortfolioDocument.empty()).sourceSnapshot(PortfolioSourceSnapshot.empty())
                .sourceHash("hash").version(1).build();
    }

    private PortfolioExport export(Portfolio portfolio) {
        PortfolioExportDocument document = new PortfolioExportDocument(1, portfolio.title(),
                portfolio.periodStart(), portfolio.periodEnd(), portfolio.content(), List.of());
        return PortfolioExport.builder().portfolioId(portfolio.id()).projectId(portfolio.projectId())
                .format(PortfolioExportFormat.PDF).status(PortfolioExportStatus.QUEUED)
                .idempotencyKey(UUID.randomUUID()).portfolioVersion(portfolio.version())
                .document(document).options(PortfolioExportOptions.defaults(PortfolioExportFormat.PDF))
                .attempts((short) 1).build();
    }
}
