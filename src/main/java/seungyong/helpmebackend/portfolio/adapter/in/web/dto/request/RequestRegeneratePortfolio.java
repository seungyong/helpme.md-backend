package seungyong.helpmebackend.portfolio.adapter.in.web.dto.request;

import seungyong.helpmebackend.portfolio.application.port.in.command.RegeneratePortfolioCommand;

public record RequestRegeneratePortfolio(Boolean refreshSources) {
    public RegeneratePortfolioCommand toCommand(Long userId, Long projectId, Long portfolioId) {
        return new RegeneratePortfolioCommand(
                userId, projectId, portfolioId, Boolean.TRUE.equals(refreshSources)
        );
    }
}
