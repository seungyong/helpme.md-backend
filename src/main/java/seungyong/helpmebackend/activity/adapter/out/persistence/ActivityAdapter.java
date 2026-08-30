package seungyong.helpmebackend.activity.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.activity.adapter.out.persistence.entity.ActivityJpaEntity;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivityEvidenceBatch;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.webhook.adapter.out.persistence.entity.WebhookDeliveryJpaEntity;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ActivityAdapter implements ActivityPortOut {
    private final ActivityJpaRepository activityJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveIfAbsent(Activity activity) {
        if (activityJpaRepository.existsByProject_IdAndExternalKey(
                activity.projectId(), activity.externalKey())) {
            return false;
        }
        activityJpaRepository.saveAndFlush(toJpaEntity(activity));
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveAllIfAbsent(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return 0;
        }
        Map<String, Activity> unique = new LinkedHashMap<>();
        activities.forEach(activity -> unique.putIfAbsent(activity.externalKey(), activity));
        Long projectId = activities.get(0).projectId();
        if (activities.stream().anyMatch(activity -> !projectId.equals(activity.projectId()))) {
            throw new IllegalArgumentException("activities must belong to one project");
        }
        Set<String> existingKeys = new HashSet<>();
        List<String> keys = List.copyOf(unique.keySet());
        for (int start = 0; start < keys.size(); start += 500) {
            List<String> chunk = keys.subList(start, Math.min(start + 500, keys.size()));
            activityJpaRepository.findAllByProject_IdAndExternalKeyIn(projectId, chunk)
                    .forEach(entity -> existingKeys.add(entity.getExternalKey()));
        }
        List<ActivityJpaEntity> newEntities = unique.values().stream()
                .filter(activity -> !existingKeys.contains(activity.externalKey()))
                .map(this::toJpaEntity)
                .toList();
        activityJpaRepository.saveAllAndFlush(newEntities);
        return newEntities.size();
    }

    /**
     * 해당하는 활동들을 조회하고, 페이지네이션 정보를 포함한 ActivityPage를 반환합니다.
     * 웹훅은 같은 시간에 여러 번 발생할 수 있으므로, 커서 기반 페이지네이션을 위해 발생 시간과 ID를 함께 사용합니다.
     * - 활동들은 발생 시간(occurredAt)과 ID를 기준으로 내림차순으로 정렬
     * - 필터링 조건에 따라 활동들을 제한할 수 있음
     * - 페이지네이션은 커서 기반으로 이루어지며, 다음 페이지가 존재하는 경우 nextCursor를 반환
     *
     * @param projectId          프로젝트 ID
     * @param query              검색 쿼리 (제목 또는 커밋 SHA에 대해 대소문자 구분 없이 검색)
     * @param branch             브랜치 이름
     * @param type               활동 타입 ({@link ActivityType})
     * @param from               발생 시간의 시작 날짜
     * @param to                 발생 시간의 종료 날짜
     * @param cursorOccurredAt   커서 기반 페이지네이션을 위한 발생 시간
     * @param cursorId           커서 기반 페이지네이션을 위한 활동 ID
     * @param size               페이지 크기 (한 페이지에 포함될 활동 수)
     * @param filtersApplied     필터링 조건이 적용되었는지 여부
     * @return 페이지네이션 정보와 함께 조회된 활동들을 포함한 {@link ActivityPage}
     */
    @Override
    @Transactional(readOnly = true)
    public ActivityPage findActivities(
            Long projectId,
            String query,
            String branch,
            ActivityType type,
            OffsetDateTime from,
            OffsetDateTime to,
            OffsetDateTime cursorOccurredAt,
            Long cursorId,
            int size,
            boolean filtersApplied
    ) {
        List<ActivityJpaEntity> entities = activityJpaRepository.findPage(
                projectId, query, branch, type, from, to, cursorOccurredAt, cursorId,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = entities.size() > size;
        List<ActivityJpaEntity> pageEntities = hasNext
                ? new ArrayList<>(entities.subList(0, size))
                : entities;
        List<Activity> items = pageEntities.stream().map(this::toDomain).toList();
        String nextCursor = hasNext ? encodeCursor(pageEntities.get(pageEntities.size() - 1)) : null;

        Object[] values = activityJpaRepository.summarize(
                projectId, query, branch, type, ActivityType.PUSH_COMMIT, from, to
        );
        Object[] summaryValues = values.length == 1 && values[0] instanceof Object[] nested
                ? nested : values;
        ActivityPage.Summary summary = new ActivityPage.Summary(
                number(summaryValues, 0),
                number(summaryValues, 1),
                number(summaryValues, 2),
                number(summaryValues, 3)
        );
        return new ActivityPage(items, summary, nextCursor, hasNext, filtersApplied);
    }

    /**
     * 해당 프로젝트의 활동을 조회하고, 총 개수를 포함한 ActivityEvidenceBatch를 반환합니다.
     * 활동은 발생 시간(occurredAt)을 기준으로 내림차순으로 정렬되며, 지정된 기간(from ~ to) 내의 활동들을 제한합니다.
     *
     * @param projectId 프로젝트 ID
     * @param from      발생 시간의 시작 날짜
     * @param to        발생 시간의 종료 날짜
     * @param limit     조회할 활동 증거의 최대 개수
     * @return 조회된 활동 증거와 총 개수를 포함한 {@link ActivityEvidenceBatch}
     */
    @Override
    @Transactional(readOnly = true)
    public ActivityEvidenceBatch findEvidence(
            Long projectId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit
    ) {
        List<ActivityJpaEntity> entities = new ArrayList<>(
                activityJpaRepository.findEvidence(
                        projectId, from, to, PageRequest.of(0, limit)
                )
        );
        Collections.reverse(entities);
        List<Activity> items = entities.stream().map(this::toDomain).toList();
        long totalCount = activityJpaRepository
                .countByProject_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        projectId, from, to
                );
        return new ActivityEvidenceBatch(items, totalCount);
    }

    private ActivityJpaEntity toJpaEntity(Activity activity) {
        return ActivityJpaEntity.builder()
                .project(ProjectJpaEntity.builder().id(activity.projectId()).build())
                .webhookDelivery(activity.webhookDeliveryId() == null ? null
                        : WebhookDeliveryJpaEntity.builder().id(activity.webhookDeliveryId()).build())
                .externalKey(activity.externalKey())
                .activityType(activity.type())
                .branchName(activity.branchName())
                .commitSha(activity.commitSha())
                .title(activity.title())
                .summary(activity.summary())
                .actorLogin(activity.actorLogin())
                .publicUrl(activity.publicUrl())
                .additions(activity.additions())
                .deletions(activity.deletions())
                .filesChanged(activity.filesChanged())
                .occurredAt(activity.occurredAt())
                .details(objectMapper.valueToTree(activity.details()))
                .build();
    }

    private Activity toDomain(ActivityJpaEntity entity) {
        Map<String, Object> details = objectMapper.convertValue(
                entity.getDetails(), new TypeReference<>() { }
        );
        return Activity.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .webhookDeliveryId(entity.getWebhookDelivery() == null
                        ? null : entity.getWebhookDelivery().getId())
                .externalKey(entity.getExternalKey())
                .type(entity.getActivityType())
                .branchName(entity.getBranchName())
                .commitSha(entity.getCommitSha())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .actorLogin(entity.getActorLogin())
                .publicUrl(entity.getPublicUrl())
                .additions(entity.getAdditions())
                .deletions(entity.getDeletions())
                .filesChanged(entity.getFilesChanged())
                .occurredAt(entity.getOccurredAt())
                .details(details)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String encodeCursor(ActivityJpaEntity entity) {
        String raw = entity.getOccurredAt() + "|" + entity.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private long number(Object[] values, int index) {
        return values != null && values.length > index && values[index] instanceof Number value
                ? value.longValue() : 0L;
    }
}
