package seungyong.helpmebackend.portfolio.domain.entity;

import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;

public record PortfolioGenerationResult(Long portfolioId, PortfolioStatus status, int version,
                                        boolean created, boolean asynchronous, int retryAfterSeconds) {
}
