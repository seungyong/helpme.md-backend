package seungyong.helpmebackend.portfolio.application;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;

public record PortfolioSourceBuildResult(PortfolioSourceSnapshot snapshot, String sourceHash) {
}
