package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;

public record ResponsePortfolioExportAccepted(Long exportId, String status, int portfolioVersion,
                                              String conflictAction, String location,
                                              int retryAfterSeconds) {
    public static ResponsePortfolioExportAccepted from(Long projectId, Long portfolioId,
                                                       PortfolioExport export) {
        return new ResponsePortfolioExportAccepted(export.id(), export.status().getDatabaseValue(),
                export.portfolioVersion(), export.conflictAction() == null ? null
                : export.conflictAction().getDatabaseValue(),
                "/api/v1/projects/" + projectId + "/portfolios/" + portfolioId
                        + "/exports/" + export.id(), 2);
    }
}
