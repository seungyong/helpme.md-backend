package seungyong.helpmebackend.portfolio.adapter.out.persistence.projection;

public interface PortfolioPdfAssetProjection {
    Long getExportId();
    Long getPortfolioId();
    String getStoragePath();
}
