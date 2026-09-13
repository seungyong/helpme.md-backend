package seungyong.helpmebackend.portfolio.application.port.in.command;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;

import java.util.UUID;

public record StartPortfolioExportCommand(Long userId, Long projectId, Long portfolioId,
                                           UUID idempotencyKey, PortfolioExportFormat format,
                                           int portfolioVersion, String notionParentPageId,
                                           PortfolioExportOptions options) {
}
