package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioGenerationResult;

public record ResponsePortfolioGeneration(Long portfolioId, String status, int version,
                                          String location, int retryAfterSeconds) {
    public static ResponsePortfolioGeneration from(Long projectId, PortfolioGenerationResult result) {
        return new ResponsePortfolioGeneration(
                result.portfolioId(), result.status().getDatabaseValue(), result.version(),
                "/api/v1/projects/%d/portfolios/%d".formatted(projectId, result.portfolioId()),
                result.retryAfterSeconds()
        );
    }
}
