package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioGenerationResult;

public record ResponsePortfolioRegeneration(Long portfolioId, String status, String location,
                                            int retryAfterSeconds) {
    public static ResponsePortfolioRegeneration from(Long projectId, PortfolioGenerationResult result) {
        return new ResponsePortfolioRegeneration(
                result.portfolioId(), result.status().getDatabaseValue(),
                "/api/v1/projects/%d/portfolios/%d".formatted(projectId, result.portfolioId()),
                result.retryAfterSeconds()
        );
    }
}
