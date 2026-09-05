package seungyong.helpmebackend.portfolio.domain.entity;

import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PortfolioPage(List<Item> items, PortfolioEligibility eligibility, String nextCursor, boolean hasNext) {
    public record Item(Long id, String title, LocalDate periodStart, LocalDate periodEnd, PortfolioTone tone,
                       PortfolioStatus status, int sectionCount, int reflectionCount, int evidenceCount,
                       int version, OffsetDateTime updatedAt, PortfolioLastExportSummary lastExportSummary,
                       Portfolio.PortfolioError error) {
    }
}
