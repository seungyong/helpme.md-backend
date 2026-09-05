package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;

import java.time.OffsetDateTime;

public record ResponseSavedPortfolio(Long id, String title, String tone, PortfolioDocument content,
                                     String status, int version, OffsetDateTime savedAt,
                                     OffsetDateTime updatedAt) {
    public static ResponseSavedPortfolio from(Portfolio portfolio) {
        return new ResponseSavedPortfolio(
                portfolio.id(), portfolio.title(), portfolio.tone().getDatabaseValue(), portfolio.content(),
                portfolio.status().getDatabaseValue(), portfolio.version(), portfolio.savedAt(), portfolio.updatedAt()
        );
    }
}
