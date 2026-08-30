package seungyong.helpmebackend.reflection.domain.entity;

import java.time.LocalDate;
import java.util.List;

public record ReflectionSourceSnapshot(
        int activityCount,
        int devlogCount,
        List<Evidence> evidence,
        Integer expectedDailyCount,
        Integer savedDailyCount,
        List<LocalDate> missingDailyDates,
        int fallbackActivityCount,
        List<DailyReflectionSource> dailyReflections,
        boolean collectionGap
) {
    public ReflectionSourceSnapshot {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        missingDailyDates = missingDailyDates == null ? List.of() : List.copyOf(missingDailyDates);
        dailyReflections = dailyReflections == null ? List.of() : List.copyOf(dailyReflections);
    }

    public static ReflectionSourceSnapshot empty() {
        return new ReflectionSourceSnapshot(
                0, 0, List.of(), null, null, List.of(), 0, List.of(), false
        );
    }

    public boolean hasSource() {
        return activityCount > 0 || devlogCount > 0
                || dailyReflections.stream().anyMatch(DailyReflectionSource::included);
    }

    public record Evidence(String ref, String title, String label, String content) {
    }

    public record DailyReflectionSource(
            Long reflectionId,
            LocalDate date,
            String title,
            String status,
            boolean included,
            String reason,
            String content
    ) {
    }
}
