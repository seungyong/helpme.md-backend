package seungyong.helpmebackend.portfolio.domain.entity;

import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;

public record PortfolioSourceData(List<ReflectionData> reflections, List<ActivityData> activities) {
    public record ReflectionData(Long id, ReflectionKind kind, LocalDate periodStart, LocalDate periodEnd,
                                 String title, int version, ReflectionDocument content) {
    }

    public record ActivityData(Long id, ActivityType type, String branchName, String commitSha,
                               String title, String publicUrl) {
    }
}
