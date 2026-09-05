package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResponsePortfolioDetail(Long id, String title, LocalDate periodStart, LocalDate periodEnd,
                                      String tone, String status, PortfolioDocument content,
                                      SourceSummary sourceSummary, boolean sourceChanged, int version,
                                      OffsetDateTime generatedAt, OffsetDateTime savedAt, Error error) {
    public static ResponsePortfolioDetail from(Portfolio portfolio) {
        boolean pendingWithoutDocument = portfolio.generatedAt() == null
                && portfolio.content().sections().isEmpty()
                && (portfolio.isGenerating() || portfolio.error() != null);
        return new ResponsePortfolioDetail(
                portfolio.id(), portfolio.title(), portfolio.periodStart(), portfolio.periodEnd(),
                portfolio.tone().getDatabaseValue(), portfolio.status().getDatabaseValue(),
                pendingWithoutDocument ? null : portfolio.content(),
                new SourceSummary(
                        portfolio.sourceSnapshot().reflections().size(),
                        portfolio.sourceSnapshot().activities().size(),
                        portfolio.sourceSnapshot().customLinks().size()
                ),
                portfolio.sourceChanged(), portfolio.version(), portfolio.generatedAt(), portfolio.savedAt(),
                Error.from(portfolio.error())
        );
    }

    public record SourceSummary(int reflectionCount, int activityCount, int customLinkCount) {
    }

    public record Error(String code, String message, boolean retryable) {
        private static Error from(Portfolio.PortfolioError error) {
            return error == null ? null : new Error(error.code(), error.message(), error.retryable());
        }
    }
}
