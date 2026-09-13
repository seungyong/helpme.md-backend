package seungyong.helpmebackend.portfolio.application.port.out;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;

public interface PortfolioPdfPortOut {
    RenderedPdf render(PortfolioExportDocument document, PortfolioExportOptions options);
}
