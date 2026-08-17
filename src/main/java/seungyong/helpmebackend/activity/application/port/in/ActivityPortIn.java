package seungyong.helpmebackend.activity.application.port.in;

import seungyong.helpmebackend.activity.domain.entity.ActivityPage;

import java.time.LocalDate;

public interface ActivityPortIn {
    ActivityPage getActivities(
            Long userId,
            Long projectId,
            String query,
            String branch,
            String type,
            LocalDate from,
            LocalDate to,
            String cursor,
            Integer size
    );
}
