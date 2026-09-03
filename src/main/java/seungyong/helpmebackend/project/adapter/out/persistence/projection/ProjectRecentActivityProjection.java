package seungyong.helpmebackend.project.adapter.out.persistence.projection;

import seungyong.helpmebackend.activity.domain.type.ActivityType;

import java.time.OffsetDateTime;

public interface ProjectRecentActivityProjection {
    Long getId();

    ActivityType getActivityType();

    String getTitle();

    String getSummary();

    String getBranchName();

    String getCommitSha();

    Integer getFilesChanged();

    OffsetDateTime getOccurredAt();
}
