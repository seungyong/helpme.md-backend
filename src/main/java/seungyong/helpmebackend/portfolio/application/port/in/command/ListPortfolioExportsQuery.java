package seungyong.helpmebackend.portfolio.application.port.in.command;

import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;

public record ListPortfolioExportsQuery(Long userId, Long projectId, Long portfolioId,
                                        PortfolioExportFormat format, PortfolioExportStatus status,
                                        String cursor, Integer size) {
}
