package seungyong.helpmebackend.portfolio.application.port.out;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.global.application.pagination.CursorPagination;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioExportPortOut {
    PortfolioExport save(PortfolioExport export);
    Optional<PortfolioExport> getByIdempotencyKey(UUID idempotencyKey);
    Optional<PortfolioExport> getByPortfolioIdAndId(Long portfolioId, Long exportId);
    List<PortfolioExport> findPage(Long portfolioId, PortfolioExportFormat format,
                                   PortfolioExportStatus status,
                                   CursorPagination<OffsetDateTime> pagination);
    Optional<PortfolioExport> claimNext(OffsetDateTime now, OffsetDateTime stuckBefore);
    void completePdf(Long exportId, String storagePath, String fileName, long fileSizeBytes,
                     int pageCount, OffsetDateTime expiresAt, OffsetDateTime completedAt);
    void completeNotion(Long exportId, String pageId, String pageUrl, OffsetDateTime completedAt);
    void requireConflictAction(Long exportId, String pageId, String pageTitle, String pageUrl);
    Optional<PortfolioExport> retry(Long exportId);
    Optional<PortfolioExport> resolveConflict(Long exportId, PortfolioConflictAction action);
    void fail(Long exportId, String errorCode, String errorMessage, OffsetDateTime completedAt);
    List<PortfolioExport> findExpiredPdf(OffsetDateTime now, int limit);
    List<String> findPdfStoragePathsByProjectId(Long projectId);
    void markExpired(Long exportId);
}
