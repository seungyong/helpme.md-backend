package seungyong.helpmebackend.portfolio.domain.entity;

import lombok.Builder;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record Portfolio(Long id, Long projectId, UUID requestKey, String title, LocalDate periodStart,
                        LocalDate periodEnd, PortfolioTone tone, PortfolioStatus status,
                        PortfolioDocument content, PortfolioSourceSnapshot sourceSnapshot, String sourceHash,
                        short generationAttempts, OffsetDateTime generationStartedAt, OffsetDateTime generatedAt,
                        OffsetDateTime savedAt, PortfolioError error, int version,
                        OffsetDateTime createdAt, OffsetDateTime updatedAt, boolean sourceChanged) {
    public Portfolio {
        if (projectId == null || requestKey == null || title == null || title.isBlank()
                || periodStart == null || periodEnd == null || periodStart.isAfter(periodEnd)
                || tone == null || status == null || content == null || sourceSnapshot == null
                || sourceHash == null || sourceHash.isBlank() || generationAttempts < 0 || version < 0) {
            throw new IllegalArgumentException("invalid portfolio");
        }
    }

    public boolean isGenerating() {
        return status == PortfolioStatus.QUEUED || status == PortfolioStatus.GENERATING;
    }

    public record PortfolioError(String code, String message, boolean retryable) {
    }
}
