package seungyong.helpmebackend.portfolio.domain.entity;

import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;

public record PortfolioSourceCatalog(PortfolioEligibility eligibility, List<ReflectionCandidate> reflections,
                                     List<ActivityCandidate> evidenceCandidates, Defaults defaults) {
    public record ReflectionCandidate(Long id, ReflectionKind kind, LocalDate periodStart, LocalDate periodEnd,
                                      String title, int version, boolean selectedByDefault) {
    }

    public record ActivityCandidate(Long activityId, String type, String title, String label, String publicUrl,
                                    boolean selectable, String unavailableReason) {
    }

    public record Defaults(LocalDate periodStart, LocalDate periodEnd, PortfolioTone tone) {
    }
}
