package seungyong.helpmebackend.portfolio.domain.entity;

public final class PortfolioPdfStoragePath {
    private PortfolioPdfStoragePath() {}

    public static String of(Long portfolioId, Long exportId) {
        return "portfolios/" + portfolioId + "/" + exportId + ".pdf";
    }
}
