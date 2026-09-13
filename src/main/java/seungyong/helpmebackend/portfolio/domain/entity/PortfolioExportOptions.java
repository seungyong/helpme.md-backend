package seungyong.helpmebackend.portfolio.domain.entity;

import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;

public record PortfolioExportOptions(
        boolean includeCoverAndToc,
        boolean includeEvidenceLinks,
        boolean includePageNumbers,
        boolean showGeneratedAt,
        boolean checkTitleConflict
) {
    public static PortfolioExportOptions defaults(PortfolioExportFormat format) {
        return format == PortfolioExportFormat.PDF
                ? new PortfolioExportOptions(true, true, true, false, false)
                : new PortfolioExportOptions(false, false, false, false, true);
    }
}
