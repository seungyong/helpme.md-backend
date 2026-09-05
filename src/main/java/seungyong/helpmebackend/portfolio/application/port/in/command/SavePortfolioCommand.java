package seungyong.helpmebackend.portfolio.application.port.in.command;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;

public record SavePortfolioCommand(Long userId, Long projectId, Long portfolioId, String title,
                                   String tone, PortfolioDocument content, Integer version) {
}
