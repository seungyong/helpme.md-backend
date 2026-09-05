package seungyong.helpmebackend.portfolio.application.port.out;

import seungyong.helpmebackend.portfolio.application.port.out.result.PortfolioCreateResult;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioLastExportSummary;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioPortOut {
    PortfolioCreateResult createIfAbsent(Portfolio portfolio);

    Optional<Portfolio> getByProjectIdAndId(Long projectId, Long portfolioId);

    Optional<Portfolio> getByProjectIdAndRequestKey(Long projectId, UUID requestKey);

    List<Portfolio> findPage(Long projectId, PortfolioStatus status, OffsetDateTime cursorUpdatedAt,
                             Long cursorId, int limit);

    Map<Long, PortfolioLastExportSummary> findLatestExportSummaries(List<Long> portfolioIds);

    Optional<Portfolio> saveIfVersionMatches(Long projectId, Long portfolioId, String title,
                                              PortfolioTone tone, PortfolioDocument content,
                                              int expectedVersion, OffsetDateTime savedAt);

    Optional<Portfolio> queueRegeneration(Long projectId, Long portfolioId,
                                           PortfolioSourceSnapshot sourceSnapshot, String sourceHash);

    Optional<Portfolio> claimNext(OffsetDateTime now, OffsetDateTime stuckBefore);

    void completeGeneration(Long portfolioId, PortfolioDocument content, OffsetDateTime generatedAt);

    void failGeneration(Long portfolioId, String errorCode, String errorMessage);
}
