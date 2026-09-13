package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.application.pagination.CursorPage;
import seungyong.helpmebackend.global.application.pagination.CursorPagination;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.notion.domain.exception.NotionErrorCode;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioExportPortIn;
import seungyong.helpmebackend.portfolio.application.port.in.command.ListPortfolioExportsQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.StartPortfolioExportCommand;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDownload;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportPage;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportErrorCode;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PortfolioExportService implements PortfolioExportPortIn {
    private final ProjectAccessResolver projectAccessResolver;
    private final PortfolioPortOut portfolioPortOut;
    private final PortfolioExportPortOut exportPortOut;
    private final NotionConnectionPortOut notionConnectionPortOut;
    private final PortfolioStoragePortOut storagePortOut;

    @Value("${portfolio.export.download-url-ttl-seconds:300}")
    private long downloadUrlTtlSeconds;

    @Override
    public PortfolioExportPage getExports(ListPortfolioExportsQuery query) {
        verifyAccess(query.userId(), query.projectId(), query.portfolioId());

        CursorPagination<OffsetDateTime> pagination =
                CursorPagination.offsetDateTime(query.cursor(), query.size());
        List<PortfolioExport> fetched = exportPortOut.findPage(
                query.portfolioId(), query.format(), query.status(), pagination
        );
        CursorPage<PortfolioExport> page = pagination.page(
                fetched, PortfolioExport::createdAt, PortfolioExport::id
        );

        return new PortfolioExportPage(
                page.items(), page.nextCursor(), page.hasNext()
        );
    }

    @Override
    public PortfolioExport getExport(Long userId, Long projectId, Long portfolioId, Long exportId) {
        verifyAccess(userId, projectId, portfolioId);
        return findExport(portfolioId, exportId);
    }

    @Override
    @Transactional
    public PortfolioExport startExport(StartPortfolioExportCommand command) {
        Project project = projectAccessResolver.resolveActive(command.userId(), command.projectId());

        // 동일 키 재전송은 이미 고정된 내보내기 결과를 반환하고, 다른 요청 재사용만 충돌 처리
        PortfolioExport existing = exportPortOut.getByIdempotencyKey(command.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (sameRequest(existing, command)) return existing;
            throw new CustomException(GlobalErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        Portfolio portfolio = portfolioPortOut.getByProjectIdAndId(command.projectId(), command.portfolioId())
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
        if (portfolio.version() != command.portfolioVersion()) {
            throw new CustomException(seungyong.helpmebackend.global.exception.DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
        }
        if (portfolio.status() != PortfolioStatus.DRAFT && portfolio.status() != PortfolioStatus.SAVED) {
            throw new CustomException(PortfolioExportErrorCode.INVALID_EXPORT_STATE);
        }

        PortfolioExportOptions options = command.options() == null
                ? PortfolioExportOptions.defaults(command.format()) : command.options();
        Long notionConnectionId = null;
        String notionParentPageId = null;
        if (command.format() == PortfolioExportFormat.NOTION) {
            NotionConnection connection = notionConnectionPortOut.getByUserId(command.userId())
                    .filter(NotionConnection::isConnected)
                    .orElseThrow(() -> new CustomException(NotionErrorCode.NOTION_CONNECTION_NOT_FOUND));

            notionConnectionId = connection.getId();
            notionParentPageId = StringUtils.hasText(command.notionParentPageId())
                    ? command.notionParentPageId().trim() : connection.getDefaultParentPageId();

            if (!StringUtils.hasText(notionParentPageId)) {
                throw new CustomException(NotionErrorCode.NOTION_PARENT_PAGE_NOT_FOUND);
            }
        }

        // 수정 중인 포트폴리오와 분리되도록 문서·근거·버전을 요청 트랜잭션에서 불변 snapshot으로 저장
        PortfolioExportDocument document = snapshot(portfolio, project.isPrivateRepository());
        return exportPortOut.save(PortfolioExport.builder()
                .portfolioId(portfolio.id()).projectId(command.projectId()).notionConnectionId(notionConnectionId)
                .format(command.format()).status(PortfolioExportStatus.QUEUED)
                .idempotencyKey(command.idempotencyKey()).portfolioVersion(portfolio.version())
                .document(document).options(options).notionParentPageId(notionParentPageId)
                .attempts((short) 1).build());
    }

    @Override
    public PortfolioDownload getDownload(Long userId, Long projectId, Long portfolioId, Long exportId) {
        verifyAccess(userId, projectId, portfolioId);

        PortfolioExport export = findExport(portfolioId, exportId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (export.isExpired(now)) throw new CustomException(PortfolioExportErrorCode.PDF_EXPIRED);
        if (export.format() != PortfolioExportFormat.PDF || export.status() != PortfolioExportStatus.SUCCEEDED
                || !StringUtils.hasText(export.storagePath())) {
            throw new CustomException(PortfolioExportErrorCode.INVALID_EXPORT_STATE);
        }

        Duration ttl = Duration.ofSeconds(downloadUrlTtlSeconds);
        return new PortfolioDownload(storagePortOut.createSignedUrl(export.storagePath(), ttl),
                export.fileName(), now.plus(ttl));
    }

    @Override
    public PortfolioExport retryExport(Long userId, Long projectId, Long portfolioId, Long exportId) {
        verifyAccess(userId, projectId, portfolioId);
        findExport(portfolioId, exportId);
        return exportPortOut.retry(exportId)
                .orElseThrow(() -> new CustomException(PortfolioExportErrorCode.INVALID_EXPORT_STATE));
    }

    @Override
    public PortfolioExport resolveConflict(Long userId, Long projectId, Long portfolioId, Long exportId,
                                           PortfolioConflictAction action) {
        verifyAccess(userId, projectId, portfolioId);

        PortfolioExport export = findExport(portfolioId, exportId);
        if (export.format() != PortfolioExportFormat.NOTION || action == null) {
            throw new CustomException(PortfolioExportErrorCode.INVALID_EXPORT_STATE);
        }

        return exportPortOut.resolveConflict(exportId, action)
                .orElseThrow(() -> new CustomException(PortfolioExportErrorCode.INVALID_EXPORT_STATE));
    }

    private void verifyAccess(Long userId, Long projectId, Long portfolioId) {
        projectAccessResolver.resolveActive(userId, projectId);
        portfolioPortOut.getByProjectIdAndId(projectId, portfolioId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private PortfolioExport findExport(Long portfolioId, Long exportId) {
        return exportPortOut.getByPortfolioIdAndId(portfolioId, exportId)
                .orElseThrow(() -> new CustomException(PortfolioExportErrorCode.EXPORT_NOT_FOUND));
    }

    private PortfolioExportDocument snapshot(Portfolio portfolio, boolean privateRepository) {
        List<PortfolioExportDocument.EvidenceLink> links = new ArrayList<>();
        PortfolioSourceSnapshot sources = portfolio.sourceSnapshot();
        if (privateRepository && !sources.activities().isEmpty()) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
        }
        for (PortfolioSourceSnapshot.ActivitySource source : sources.activities()) {
            links.add(new PortfolioExportDocument.EvidenceLink(
                    "activity:" + source.id(), source.label(), source.publicUrl()));
        }
        for (int index = 0; index < sources.customLinks().size(); index++) {
            PortfolioSourceSnapshot.CustomLink source = sources.customLinks().get(index);
            links.add(new PortfolioExportDocument.EvidenceLink(
                    "custom:" + index, source.label(), source.url()));
        }
        return new PortfolioExportDocument(PortfolioExportDocument.CURRENT_SCHEMA_VERSION,
                portfolio.title(), portfolio.periodStart(), portfolio.periodEnd(), portfolio.content(), links);
    }

    private boolean sameRequest(PortfolioExport existing, StartPortfolioExportCommand command) {
        PortfolioExportOptions options = command.options() == null
                ? PortfolioExportOptions.defaults(command.format()) : command.options();

        return Objects.equals(existing.projectId(), command.projectId())
                && Objects.equals(existing.portfolioId(), command.portfolioId())
                && existing.format() == command.format()
                && existing.portfolioVersion() == command.portfolioVersion()
                && (!StringUtils.hasText(command.notionParentPageId())
                    || Objects.equals(existing.notionParentPageId(), command.notionParentPageId().trim()))
                && Objects.equals(existing.options(), options);
    }

}
