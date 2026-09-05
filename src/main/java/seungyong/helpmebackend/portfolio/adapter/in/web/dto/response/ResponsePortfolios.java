package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioEligibility;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioPage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ResponsePortfolios(List<Item> items, Eligibility eligibility, Page page) {
    public static ResponsePortfolios from(PortfolioPage source) {
        return new ResponsePortfolios(
                source.items().stream().map(Item::from).toList(),
                Eligibility.from(source.eligibility()),
                new Page(source.nextCursor(), source.hasNext())
        );
    }

    public record Item(Long id, String title, LocalDate periodStart, LocalDate periodEnd, String tone,
                       String status, int sectionCount, int reflectionCount, int evidenceCount, int version,
                       OffsetDateTime updatedAt, LastExportSummary lastExportSummary, Error error) {
        private static Item from(PortfolioPage.Item source) {
            return new Item(source.id(), source.title(), source.periodStart(), source.periodEnd(),
                    source.tone().getDatabaseValue(), source.status().getDatabaseValue(), source.sectionCount(),
                    source.reflectionCount(), source.evidenceCount(), source.version(), source.updatedAt(),
                    LastExportSummary.from(source.lastExportSummary()), Error.from(source.error()));
        }
    }

    public record LastExportSummary(String format, String status, OffsetDateTime completedAt) {
        private static LastExportSummary from(seungyong.helpmebackend.portfolio.domain.entity.PortfolioLastExportSummary source) {
            return source == null ? null : new LastExportSummary(
                    source.format().getDatabaseValue(), source.status().getDatabaseValue(), source.completedAt()
            );
        }
    }

    public record Eligibility(boolean canCreate, String reason, int requiredSavedReflectionCount,
                              long currentSavedReflectionCount) {
        private static Eligibility from(PortfolioEligibility source) {
            return new Eligibility(source.canCreate(), source.reason(), source.requiredSavedReflectionCount(),
                    source.currentSavedReflectionCount());
        }
    }

    public record Page(String nextCursor, boolean hasNext) {
    }

    public record Error(String code, String message, boolean retryable) {
        private static Error from(Portfolio.PortfolioError error) {
            return error == null ? null : new Error(error.code(), error.message(), error.retryable());
        }
    }
}
