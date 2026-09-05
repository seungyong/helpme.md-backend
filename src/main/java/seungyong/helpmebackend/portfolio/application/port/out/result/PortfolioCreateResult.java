package seungyong.helpmebackend.portfolio.application.port.out.result;

import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;

public record PortfolioCreateResult(Portfolio portfolio, boolean created) {
}
