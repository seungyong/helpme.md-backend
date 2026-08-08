package seungyong.helpmebackend.project.adapter.in.web.dto.response;

import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;

import java.time.OffsetDateTime;
import java.util.List;

public record ResponseUpdatedProjectSettings(
        List<String> trackedBranches,
        boolean trackAllBranches,
        String timezone,
        ResponseProjectSettings.Daily daily,
        ResponseProjectSettings.Weekly weekly,
        short webhookPayloadRetentionDays,
        OffsetDateTime updatedAt
) {
    public static ResponseUpdatedProjectSettings from(Project project) {
        ProjectSettings settings = project.getSettings();
        return new ResponseUpdatedProjectSettings(
                settings.trackedBranches(),
                settings.trackAllBranches(),
                settings.timezone(),
                ResponseProjectSettings.Daily.from(settings.daily()),
                ResponseProjectSettings.Weekly.from(settings.weekly()),
                settings.webhookPayloadRetentionDays(),
                project.getUpdatedAt()
        );
    }
}
