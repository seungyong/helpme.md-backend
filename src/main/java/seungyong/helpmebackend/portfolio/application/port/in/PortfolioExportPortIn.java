package seungyong.helpmebackend.portfolio.application.port.in;

import seungyong.helpmebackend.portfolio.application.port.in.command.ListPortfolioExportsQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.StartPortfolioExportCommand;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDownload;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportPage;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;

public interface PortfolioExportPortIn {
    PortfolioExportPage getExports(ListPortfolioExportsQuery query);
    PortfolioExport getExport(Long userId, Long projectId, Long portfolioId, Long exportId);
    PortfolioExport startExport(StartPortfolioExportCommand command);
    PortfolioDownload getDownload(Long userId, Long projectId, Long portfolioId, Long exportId);
    PortfolioExport retryExport(Long userId, Long projectId, Long portfolioId, Long exportId);
    PortfolioExport resolveConflict(Long userId, Long projectId, Long portfolioId, Long exportId,
                                    PortfolioConflictAction action);
}
