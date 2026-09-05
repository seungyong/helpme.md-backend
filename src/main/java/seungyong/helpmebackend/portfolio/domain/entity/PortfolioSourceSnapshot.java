package seungyong.helpmebackend.portfolio.domain.entity;

import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;

public record PortfolioSourceSnapshot(List<ReflectionSource> reflections, List<ActivitySource> activities, List<CustomLink> customLinks) {
    public PortfolioSourceSnapshot {
        reflections = reflections == null ? List.of() : List.copyOf(reflections);
        activities = activities == null ? List.of() : List.copyOf(activities);
        customLinks = customLinks == null ? List.of() : List.copyOf(customLinks);
    }

    public static PortfolioSourceSnapshot empty() {
        return new PortfolioSourceSnapshot(List.of(), List.of(), List.of());
    }

    public record ReflectionSource(Long id, ReflectionKind kind, LocalDate periodStart, LocalDate periodEnd, String title, int version, ReflectionDocument content) {
    }

    public record ActivitySource(Long id, ActivityType type, String title, String label, String publicUrl) {
    }

    public record CustomLink(String label, String url) {
    }
}
