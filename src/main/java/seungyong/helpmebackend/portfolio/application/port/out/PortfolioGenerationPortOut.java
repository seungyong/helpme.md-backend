package seungyong.helpmebackend.portfolio.application.port.out;

import seungyong.helpmebackend.portfolio.application.port.out.result.GeneratedPortfolio;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;

public interface PortfolioGenerationPortOut {
    GeneratedPortfolio generate(Portfolio portfolio);
}
