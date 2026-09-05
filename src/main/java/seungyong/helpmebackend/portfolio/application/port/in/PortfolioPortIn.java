package seungyong.helpmebackend.portfolio.application.port.in;

import seungyong.helpmebackend.portfolio.application.port.in.command.CreatePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.in.command.GetPortfolioSourcesQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.ListPortfoliosQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.RegeneratePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.in.command.SavePortfolioCommand;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioGenerationResult;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioPage;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceCatalog;

public interface PortfolioPortIn {
    PortfolioSourceCatalog getSources(GetPortfolioSourcesQuery query);

    PortfolioPage getPortfolios(ListPortfoliosQuery query);

    Portfolio getPortfolio(Long userId, Long projectId, Long portfolioId);

    PortfolioGenerationResult createPortfolio(CreatePortfolioCommand command);

    Portfolio savePortfolio(SavePortfolioCommand command);

    PortfolioGenerationResult regeneratePortfolio(RegeneratePortfolioCommand command);
}
