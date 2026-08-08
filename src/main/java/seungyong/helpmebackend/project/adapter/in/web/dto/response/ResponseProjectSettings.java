package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record ResponseProjectSettings(
        List<String> trackedBranches,
        boolean trackAllBranches,
        String timezone,
        Daily daily,
        Weekly weekly,
        short webhookPayloadRetentionDays,
        ResponseProject.Sync sync,
        ResponseProject.Webhook webhook,
        String status
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static ResponseProjectSettings from(Project project) {
        ProjectSettings settings = project.getSettings();
        return new ResponseProjectSettings(
                settings.trackedBranches(),
                settings.trackAllBranches(),
                settings.timezone(),
                Daily.from(settings.daily()),
                Weekly.from(settings.weekly()),
                settings.webhookPayloadRetentionDays(),
                ResponseProject.Sync.from(project.getSync()),
                ResponseProject.Webhook.from(project.getWebhook()),
                project.getStatus().getDatabaseValue()
        );
    }

    public record Daily(boolean enabled, String generationTime) {
        static Daily from(ProjectSettings.DailyReflectionSchedule schedule) {
            return new Daily(
                    schedule.enabled(),
                    schedule.generationTime().format(TIME_FORMATTER)
            );
        }
    }

    public record Weekly(
            boolean enabled,
            @Schema(
                    description = "주간 회고 생성 요일",
                    allowableValues = {
                            "sunday", "monday", "tuesday", "wednesday",
                            "thursday", "friday", "saturday"
                    }
            )
            String generationDay,
            String generationTime
    ) {
        static Weekly from(ProjectSettings.WeeklyReflectionSchedule schedule) {
            return new Weekly(
                    schedule.enabled(),
                    schedule.generationDay().getApiValue(),
                    schedule.generationTime().format(TIME_FORMATTER)
            );
        }
    }
}
