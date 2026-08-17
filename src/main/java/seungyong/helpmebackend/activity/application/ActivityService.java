package seungyong.helpmebackend.activity.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.activity.application.port.in.ActivityPortIn;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ActivityService implements ActivityPortIn {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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
        int normalizedSize = normalizeSize(size);
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

        // cursor를 디코딩하여 발생 시간과 ID를 추출
        Cursor decodedCursor = decodeCursor(cursor);
        boolean filtersApplied = normalizedQuery != null || normalizedBranch != null
                || normalizedType != null || from != null || to != null || cursor != null;

        return activityPortOut.findActivities(
                projectId,
                normalizedQuery,
                normalizedBranch,
                normalizedType,
                normalizedFrom.atStartOfDay(zoneId).toOffsetDateTime(),
                normalizedTo.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime(),
                decodedCursor.occurredAt(),
                decodedCursor.id(),
                normalizedSize,
                filtersApplied
        );
    }

    private int normalizeSize(Integer size) {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < 1 || value > MAX_SIZE) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        return value;
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

    private Cursor decodeCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return new Cursor(null, null);
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", 2);
            return new Cursor(OffsetDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private record Cursor(OffsetDateTime occurredAt, Long id) {
    }
}
