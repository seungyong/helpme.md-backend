package seungyong.helpmebackend.portfolio.domain.entity;

import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;

import java.time.OffsetDateTime;

public record PortfolioLastExportSummary(
        PortfolioExportFormat format,
        PortfolioExportStatus status,
        OffsetDateTime completedAt
) {
}
