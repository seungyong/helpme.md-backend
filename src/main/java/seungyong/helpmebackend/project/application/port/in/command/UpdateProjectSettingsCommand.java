package seungyong.helpmebackend.project.application.port.in.command;

import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;

import java.time.LocalTime;
import java.util.List;

public record UpdateProjectSettingsCommand(
        Long userId,
        Long projectId,
        List<String> trackedBranches,
        Boolean trackAllBranches,
        String timezone,
        Boolean dailyEnabled,
        LocalTime dailyGenerationTime,
        Boolean weeklyEnabled,
        ReflectionWeekday weeklyGenerationDay,
        LocalTime weeklyGenerationTime,
        Short webhookPayloadRetentionDays
) {
    public UpdateProjectSettingsCommand {
        trackedBranches = trackedBranches == null ? null : List.copyOf(trackedBranches);
    }

    public boolean hasChanges() {
        return trackedBranches != null
                || trackAllBranches != null
                || timezone != null
                || dailyEnabled != null
                || dailyGenerationTime != null
                || weeklyEnabled != null
                || weeklyGenerationDay != null
                || weeklyGenerationTime != null
                || webhookPayloadRetentionDays != null;
    }
}
