package seungyong.helpmebackend.activity.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.activity.application.port.in.ActivityPortIn;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.application.pagination.CursorPagination;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ActivityService implements ActivityPortIn {
    private final ProjectAccessResolver projectAccessResolver;
    private final ActivityPortOut activityPortOut;

    @Override
    public ActivityPage getActivities(
            Long userId,
            Long projectId,
            String query,
            String branch,
            String type,
            LocalDate from,
            LocalDate to,
            String cursor,
            Integer size
    ) {
        Project project = projectAccessResolver.resolveActive(userId, projectId);
        CursorPagination<OffsetDateTime> pagination =
                CursorPagination.offsetDateTime(cursor, size);
        String normalizedQuery = normalizeText(query);
        String normalizedBranch = normalizeText(branch);
        ActivityType normalizedType = normalizeType(type);

        ZoneId zoneId = ZoneId.of(project.getSettings().timezone());
        LocalDate today = LocalDate.now(zoneId);

        // from이 null이면 오늘 기준 6일 전, to가 null이면 오늘로 설정
        LocalDate normalizedFrom = from == null ? today.minusDays(6) : from;
        LocalDate normalizedTo = to == null ? today : to;

        if (normalizedFrom.isAfter(normalizedTo)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }

        boolean filtersApplied = normalizedQuery != null || normalizedBranch != null
                || normalizedType != null || from != null || to != null || cursor != null;

        return activityPortOut.findActivities(
                projectId,
                normalizedQuery,
                normalizedBranch,
                normalizedType,
                normalizedFrom.atStartOfDay(zoneId).toOffsetDateTime(),
                normalizedTo.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime(),
                pagination,
                filtersApplied
        );
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ActivityType normalizeType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ActivityType.fromDatabaseValue(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

}
