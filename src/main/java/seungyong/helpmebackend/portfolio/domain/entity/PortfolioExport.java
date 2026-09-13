package seungyong.helpmebackend.portfolio.domain.entity;

import lombok.Builder;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record PortfolioExport(
        Long id,
        Long portfolioId,
        Long projectId,
        Long notionConnectionId,
        PortfolioExportFormat format,
        PortfolioExportStatus status,
        UUID idempotencyKey,
        int portfolioVersion,
        PortfolioExportDocument document,
        PortfolioExportOptions options,
        String storagePath,
        String fileName,
        Long fileSizeBytes,
        Integer pageCount,
        OffsetDateTime expiresAt,
        String notionParentPageId,
        String notionPageId,
        String notionPageUrl,
        PortfolioConflictAction conflictAction,
        String conflictPageId,
        String conflictPageTitle,
        String conflictPageUrl,
        short attempts,
        String errorCode,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public boolean isExpired(OffsetDateTime now) {
        return format == PortfolioExportFormat.PDF && expiresAt != null && !expiresAt.isAfter(now);
    }

    public PortfolioExportStatus effectiveStatus(OffsetDateTime now) {
        return isExpired(now) ? PortfolioExportStatus.EXPIRED : status;
    }
}
