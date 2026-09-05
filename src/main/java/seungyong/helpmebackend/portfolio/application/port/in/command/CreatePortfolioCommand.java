package seungyong.helpmebackend.portfolio.application.port.in.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePortfolioCommand(Long userId, Long projectId, UUID idempotencyKey, String title,
                                     LocalDate periodStart, LocalDate periodEnd, String tone,
                                     List<Long> reflectionIds, List<Long> activityIds,
                                     List<CustomEvidenceLinkCommand> customEvidenceLinks,
                                     String generationMode) {
}
