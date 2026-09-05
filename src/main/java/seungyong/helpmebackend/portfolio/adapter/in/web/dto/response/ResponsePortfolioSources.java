package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioEligibility;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceCatalog;

import java.time.LocalDate;
import java.util.List;

public record ResponsePortfolioSources(Eligibility eligibility, List<Reflection> reflections,
                                       List<EvidenceCandidate> evidenceCandidates, Defaults defaults) {
    public static ResponsePortfolioSources from(PortfolioSourceCatalog source) {
        return new ResponsePortfolioSources(
                Eligibility.from(source.eligibility()),
                source.reflections().stream().map(Reflection::from).toList(),
                source.evidenceCandidates().stream().map(EvidenceCandidate::from).toList(),
                new Defaults(source.defaults().periodStart(), source.defaults().periodEnd(),
                        source.defaults().tone().getDatabaseValue())
        );
    }

    public record Eligibility(boolean canCreate, String reason, int requiredSavedReflectionCount,
                              long currentSavedReflectionCount) {
        private static Eligibility from(PortfolioEligibility source) {
            return new Eligibility(source.canCreate(), source.reason(), source.requiredSavedReflectionCount(),
                    source.currentSavedReflectionCount());
        }
    }

    public record Reflection(Long id, String kind, LocalDate periodStart, LocalDate periodEnd, String title,
                             String status, int version, boolean selectedByDefault) {
        private static Reflection from(PortfolioSourceCatalog.ReflectionCandidate source) {
            return new Reflection(source.id(), source.kind().getDatabaseValue(), source.periodStart(),
                    source.periodEnd(), source.title(), "saved", source.version(), source.selectedByDefault());
        }
    }

    public record EvidenceCandidate(Long activityId, String type, String title, String label, String publicUrl,
                                    boolean isSelectable, String unavailableReason) {
        private static EvidenceCandidate from(PortfolioSourceCatalog.ActivityCandidate source) {
            return new EvidenceCandidate(source.activityId(), source.type(), source.title(), source.label(),
                    source.publicUrl(), source.selectable(), source.unavailableReason());
        }
    }

    public record Defaults(LocalDate periodStart, LocalDate periodEnd, String tone) {
    }
}
