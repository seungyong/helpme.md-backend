package seungyong.helpmebackend.project.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import seungyong.helpmebackend.project.application.port.in.command.UpdateProjectSettingsCommand;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;

import java.time.LocalTime;
import java.util.List;

public record RequestProjectSettings(
        List<@NotBlank String> trackedBranches,
        Boolean trackAllBranches,
        String timezone,
        Boolean dailyEnabled,
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$") String dailyGenerationTime,
        Boolean weeklyEnabled,
        @Schema(
                description = "주간 회고 생성 요일",
                allowableValues = {
                        "sunday", "monday", "tuesday", "wednesday",
                        "thursday", "friday", "saturday"
                }
        )
        @Pattern(regexp = "^(sunday|monday|tuesday|wednesday|thursday|friday|saturday)$")
        String weeklyGenerationDay,
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$") String weeklyGenerationTime,
        @Min(7) @Max(30) Integer webhookPayloadRetentionDays
) {
    public UpdateProjectSettingsCommand toCommand(Long userId, Long projectId) {
        return new UpdateProjectSettingsCommand(
                userId,
                projectId,
                trackedBranches,
                trackAllBranches,
                timezone,
                dailyEnabled,
                parseTime(dailyGenerationTime),
                weeklyEnabled,
                weeklyGenerationDay == null
                        ? null
                        : ReflectionWeekday.fromApiValue(weeklyGenerationDay),
                parseTime(weeklyGenerationTime),
                webhookPayloadRetentionDays == null
                        ? null
                        : webhookPayloadRetentionDays.shortValue()
        );
    }

    private LocalTime parseTime(String value) {
        return value == null ? null : LocalTime.parse(value);
    }
}
