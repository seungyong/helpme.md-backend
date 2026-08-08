package seungyong.helpmebackend.project.domain.entity;

import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;

public record ProjectSettings(
        List<String> trackedBranches,
        boolean trackAllBranches,
        String timezone,
        DailyReflectionSchedule daily,
        WeeklyReflectionSchedule weekly,
        short webhookPayloadRetentionDays
) {
    private static final short MIN_RETENTION_DAYS = 7;
    private static final short MAX_RETENTION_DAYS = 30;

    public ProjectSettings {
        // defensive copy를 사용함으로써, 외부에서 전달된 List가 변경되더라도, ProjectSettings 내부의 List는 변경되지 않도록 보장
        trackedBranches = trackedBranches == null ? List.of() : List.copyOf(trackedBranches);

        if (trackedBranches.stream().anyMatch(branch -> branch == null || branch.isBlank())) {
            throw new IllegalArgumentException("trackedBranches must not contain blank values");
        }
        if (new LinkedHashSet<>(trackedBranches).size() != trackedBranches.size()) {
            throw new IllegalArgumentException("trackedBranches must not contain duplicates");
        }
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("timezone must not be blank");
        }
        if (daily == null || weekly == null) {
            throw new IllegalArgumentException("reflection schedules are required");
        }
        if (webhookPayloadRetentionDays < MIN_RETENTION_DAYS
                || webhookPayloadRetentionDays > MAX_RETENTION_DAYS) {
            throw new IllegalArgumentException("webhook payload retention is out of range");
        }
        if (trackAllBranches) {
            trackedBranches = List.of();
        }
    }

    public static ProjectSettings defaults() {
        return new ProjectSettings(
                List.of(),
                false,
                "Asia/Seoul",
                new DailyReflectionSchedule(true, LocalTime.of(23, 30)),
                new WeeklyReflectionSchedule(true, ReflectionWeekday.SUNDAY, LocalTime.of(23, 50)),
                (short) 30
        );
    }

    public record DailyReflectionSchedule(
            boolean enabled,
            LocalTime generationTime
    ) {
        public DailyReflectionSchedule {
            if (generationTime == null) {
                throw new IllegalArgumentException("daily generation time is required");
            }
        }
    }

    public record WeeklyReflectionSchedule(
            boolean enabled,
            ReflectionWeekday generationDay,
            LocalTime generationTime
    ) {
        public WeeklyReflectionSchedule {
            if (generationDay == null || generationTime == null) {
                throw new IllegalArgumentException("weekly generation schedule is required");
            }
        }
    }
}
