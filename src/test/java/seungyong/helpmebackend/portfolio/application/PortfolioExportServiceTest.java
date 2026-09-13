package seungyong.helpmebackend.portfolio.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.portfolio.application.port.in.command.StartPortfolioExportCommand;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportErrorCode;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioExportServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;
    private static final Long PORTFOLIO_ID = 501L;
    private static final UUID KEY = UUID.fromString("2a354265-0000-4000-8000-000000000001");

    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private PortfolioPortOut portfolioPortOut;
    @Mock private PortfolioExportPortOut exportPortOut;
    @Mock private NotionConnectionPortOut notionConnectionPortOut;
    @Mock private PortfolioStoragePortOut storagePortOut;
    private PortfolioExportService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioExportService(projectAccessResolver, portfolioPortOut, exportPortOut,
                notionConnectionPortOut, storagePortOut);
        ReflectionTestUtils.setField(service, "downloadUrlTtlSeconds", 300L);
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID))
                .willReturn(new Project(PROJECT_ID, USER_ID, "owner/repo"));
    }

    @Test
    @DisplayName("PDF 시작은 현재 version과 문서 근거를 고정 snapshot으로 저장")
    void startPdf_freezesSnapshot() {
        given(exportPortOut.getByIdempotencyKey(KEY)).willReturn(Optional.empty());
        given(portfolioPortOut.getByProjectIdAndId(PROJECT_ID, PORTFOLIO_ID)).willReturn(Optional.of(portfolio(3)));
        given(exportPortOut.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));

        PortfolioExport result = service.startExport(command(3));

        assertThat(result.id()).isEqualTo(701L);
        assertThat(result.document().title()).isEqualTo("백엔드 포트폴리오");
        assertThat(result.document().evidenceLinks()).extracting(PortfolioExportDocument.EvidenceLink::url)
                .containsExactly("https://github.com/owner/repo/commit/abc");
        assertThat(result.attempts()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("요청 version이 현재 포트폴리오와 다르면 DOCUMENT_40901")
    void startPdf_versionConflict() {
        given(exportPortOut.getByIdempotencyKey(KEY)).willReturn(Optional.empty());
        given(portfolioPortOut.getByProjectIdAndId(PROJECT_ID, PORTFOLIO_ID)).willReturn(Optional.of(portfolio(4)));

        assertThatThrownBy(() -> service.startExport(command(3)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
    }

    @Test
    @DisplayName("private Repository activity 근거가 snapshot에 남아 있으면 PORTFOLIO_42202")
    void startPdf_privateEvidence() {
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(Project.builder()
                .id(PROJECT_ID).userId(USER_ID).repoFullName("owner/private").privateRepository(true).build());
        given(exportPortOut.getByIdempotencyKey(KEY)).willReturn(Optional.empty());
        given(portfolioPortOut.getByProjectIdAndId(PROJECT_ID, PORTFOLIO_ID)).willReturn(Optional.of(portfolio(3)));

        assertThatThrownBy(() -> service.startExport(command(3)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("같은 Idempotency-Key와 같은 요청은 snapshot을 다시 만들지 않고 기존 export 반환")
    void startPdf_sameKey() {
        PortfolioExport existing = withId(export(3));
        given(exportPortOut.getByIdempotencyKey(KEY)).willReturn(Optional.of(existing));

        PortfolioExport result = service.startExport(command(3));

        assertThat(result).isSameAs(existing);
        verify(portfolioPortOut, never()).getByProjectIdAndId(any(), any());
    }

    @Test
    @DisplayName("같은 Idempotency-Key를 다른 version 요청에 사용하면 REQ_40901")
    void startPdf_idempotencyConflict() {
        given(exportPortOut.getByIdempotencyKey(KEY)).willReturn(Optional.of(withId(export(2))));

        assertThatThrownBy(() -> service.startExport(command(3)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    @DisplayName("만료 시각이 지난 PDF는 서명 URL 없이 EXPORT_41001")
    void download_expired() {
        PortfolioExport expired = PortfolioExport.builder().id(701L).portfolioId(PORTFOLIO_ID)
                .projectId(PROJECT_ID).format(PortfolioExportFormat.PDF).status(PortfolioExportStatus.SUCCEEDED)
                .document(document()).options(PortfolioExportOptions.defaults(PortfolioExportFormat.PDF))
                .storagePath("portfolios/501/701.pdf").fileName("portfolio.pdf")
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)).build();
        given(portfolioPortOut.getByProjectIdAndId(PROJECT_ID, PORTFOLIO_ID)).willReturn(Optional.of(portfolio(3)));
        given(exportPortOut.getByPortfolioIdAndId(PORTFOLIO_ID, 701L)).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.getDownload(USER_ID, PROJECT_ID, PORTFOLIO_ID, 701L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioExportErrorCode.PDF_EXPIRED);
        verify(storagePortOut, never()).createSignedUrl(any(), any());
    }

    private StartPortfolioExportCommand command(int version) {
        return new StartPortfolioExportCommand(USER_ID, PROJECT_ID, PORTFOLIO_ID, KEY,
                PortfolioExportFormat.PDF, version, null,
                PortfolioExportOptions.defaults(PortfolioExportFormat.PDF));
    }

    private Portfolio portfolio(int version) {
        return Portfolio.builder().id(PORTFOLIO_ID).projectId(PROJECT_ID).requestKey(UUID.randomUUID())
                .title("백엔드 포트폴리오").periodStart(LocalDate.of(2026, 8, 1))
                .periodEnd(LocalDate.of(2026, 8, 31)).tone(PortfolioTone.CONCISE)
                .status(PortfolioStatus.SAVED).content(new PortfolioDocument(1, List.of(
                        new PortfolioDocument.Section("summary", "summary", "요약", "구현 내용", List.of("activity:10")))))
                .sourceSnapshot(new PortfolioSourceSnapshot(List.of(), List.of(
                        new PortfolioSourceSnapshot.ActivitySource(10L, null, "커밋", "로그인 구현",
                                "https://github.com/owner/repo/commit/abc")), List.of()))
                .sourceHash("hash").version(version).build();
    }

    private PortfolioExport export(int version) {
        return PortfolioExport.builder().portfolioId(PORTFOLIO_ID).projectId(PROJECT_ID)
                .format(PortfolioExportFormat.PDF).status(PortfolioExportStatus.QUEUED)
                .idempotencyKey(KEY).portfolioVersion(version).document(document())
                .options(PortfolioExportOptions.defaults(PortfolioExportFormat.PDF)).attempts((short) 1).build();
    }

    private PortfolioExportDocument document() {
        return new PortfolioExportDocument(1, "백엔드 포트폴리오", LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), PortfolioDocument.empty(), List.of());
    }

    private PortfolioExport withId(PortfolioExport export) {
        return PortfolioExport.builder().id(701L).portfolioId(export.portfolioId()).projectId(export.projectId())
                .format(export.format()).status(export.status()).idempotencyKey(export.idempotencyKey())
                .portfolioVersion(export.portfolioVersion()).document(export.document()).options(export.options())
                .attempts(export.attempts()).build();
    }
}
