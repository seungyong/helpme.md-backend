package seungyong.helpmebackend.portfolio.domain.entity;

import java.time.LocalDate;
import java.util.List;

public record PortfolioExportDocument(
        int schemaVersion,
        String title,
        LocalDate periodStart,
        LocalDate periodEnd,
        PortfolioDocument content,
        List<EvidenceLink> evidenceLinks
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PortfolioExportDocument {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || title == null || title.isBlank()
                || periodStart == null || periodEnd == null || content == null) {
            throw new IllegalArgumentException("invalid portfolio export document");
        }
        evidenceLinks = evidenceLinks == null ? List.of() : List.copyOf(evidenceLinks);
    }

    public record EvidenceLink(String ref, String label, String url) {
    }
}
