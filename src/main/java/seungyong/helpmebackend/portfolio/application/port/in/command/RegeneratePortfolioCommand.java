package seungyong.helpmebackend.portfolio.application.port.in.command;

public record RegeneratePortfolioCommand(Long userId, Long projectId, Long portfolioId, boolean refreshSources) {
}
