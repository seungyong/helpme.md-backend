package seungyong.helpmebackend.reflection.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.activity.application.port.out.ActivityPortOut;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivityEvidenceBatch;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReflectionSourceBuilder {
    private static final int MAX_ACTIVITY_EVIDENCE = 100;

    private final ActivityPortOut activityPortOut;
    private final DevlogPortOut devlogPortOut;
    private final ReflectionPortOut reflectionPortOut;

    /**
     * 프로젝트의 회고 소스 스냅샷을 생성합니다.
     * @param project 프로젝트 엔티티
     * @param kind 회고 종류 (일일/주간)
     * @param periodStart 회고 기간 시작일
     * @param periodEnd 회고 기간 종료일
     * @return 회고 소스 스냅샷과 품질, 해시값을 포함한 결과 객체
     */
    public Result build(
            Project project,
            ReflectionKind kind,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        ZoneId zoneId = ZoneId.of(project.getSettings().timezone());
        ActivityEvidenceBatch activities = activityPortOut.findEvidence(
                project.getId(),
                periodStart.atStartOfDay(zoneId).toOffsetDateTime(),
                // 1일을 더해서 periodEnd의 23:59:59까지 포함되도록 함
                periodEnd.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime(),
                MAX_ACTIVITY_EVIDENCE
        );
        List<Devlog> devlogs = devlogPortOut.getByProjectIdAndLogDateBetween(
                project.getId(), periodStart, periodEnd
        );

        // 회고 소스 수집에 문제가 있는지 여부를 판단합니다.
        // 프로젝트의 동기화 상태가 READY가 아니거나, 웹훅 상태가 DEGRADED 또는 DISCONNECTED인 경우 collectionGap을 true로 설정합니다.
        boolean collectionGap = project.getSync().status() != ProjectSyncStatus.READY
                || project.getWebhook().status() == ProjectWebhookStatus.DEGRADED
                || project.getWebhook().status() == ProjectWebhookStatus.DISCONNECTED;

        ReflectionSourceSnapshot snapshot = kind == ReflectionKind.DAILY
                ? daily(activities, devlogs, collectionGap)
                : weekly(project, periodStart, periodEnd, activities, devlogs, collectionGap);

        // AI 입력 근거를 JSON snapshot으로 고정하고, 동일 근거 비교용 SHA-256 hash 생성
        SourceQuality quality = quality(kind, snapshot);
        return new Result(snapshot, quality, hash(snapshot));
    }

    /**
     * 일일 회고 소스 스냅샷을 생성합니다.
     * @param activityBatch 활동 증거 배치
     * @param devlogs 개발 로그 목록
     * @param collectionGap 회고 소스 수집에 문제가 있는지 여부
     * @return 생성된 ReflectionSourceSnapshot 객체
     */
    private ReflectionSourceSnapshot daily(
            ActivityEvidenceBatch activityBatch,
            List<Devlog> devlogs,
            boolean collectionGap
    ) {
        List<ReflectionSourceSnapshot.Evidence> evidence = new ArrayList<>();
        activityBatch.items().forEach(activity -> evidence.add(activityEvidence(activity)));
        devlogs.forEach(devlog -> evidence.add(devlogEvidence(devlog)));
        return new ReflectionSourceSnapshot(
                Math.toIntExact(activityBatch.totalCount()),
                devlogs.size(),
                evidence,
                null,
                null,
                List.of(),
                0,
                List.of(),
                collectionGap
        );
    }

    /**
     * 주간 회고 소스 스냅샷을 생성합니다.
     * @param project 프로젝트 엔티티
     * @param periodStart 회고 기간 시작일
     * @param periodEnd 회고 기간 종료일
     * @param activityBatch 활동 증거 배치
     * @param devlogs 개발 로그 목록
     * @param collectionGap 회고 소스 수집에 문제가 있는지 여부
     * @return 생성된 ReflectionSourceSnapshot 객체
     */
    private ReflectionSourceSnapshot weekly(
            Project project,
            LocalDate periodStart,
            LocalDate periodEnd,
            ActivityEvidenceBatch activityBatch,
            List<Devlog> devlogs,
            boolean collectionGap
    ) {
        // 회고 기간 내에 저장된 일일 회고를 날짜별로 매핑합니다.
        Map<LocalDate, Reflection> savedByDate = reflectionPortOut
                .findSavedDaily(project.getId(), periodStart, periodEnd)
                .stream()
                .collect(Collectors.toMap(Reflection::periodStart, reflection -> reflection));

        // 회고 기간 내의 활동을 날짜별로 그룹화합니다. 활동의 발생 시간(occurredAt)을 프로젝트의 시간대에 맞춰 LocalDate로 변환 및 그룹화.
        Map<LocalDate, List<Activity>> activitiesByDate = activityBatch.items().stream()
                .collect(Collectors.groupingBy(activity ->
                        activity.occurredAt().atZoneSameInstant(
                                ZoneId.of(project.getSettings().timezone())
                        ).toLocalDate()));

        List<LocalDate> missingDates = new ArrayList<>();
        List<ReflectionSourceSnapshot.DailyReflectionSource> dailySources = new ArrayList<>();
        List<ReflectionSourceSnapshot.Evidence> evidence = new ArrayList<>();
        int fallbackActivityCount = 0;

        // date를 periodStart부터 periodEnd까지 하루씩 증가시키며 반복합니다.
        for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
            Reflection saved = savedByDate.get(date);

            // 저장된 일일 회고가 존재하면, 해당 회고의 내용을 evidence에 추가하고 dailySources에 저장된 회고 정보를 추가합니다.
            if (saved != null) {
                String content = saved.content().sections().stream()
                        .map(section -> section.title() + "\n" + section.contentMd())
                        .collect(Collectors.joining("\n\n"));

                dailySources.add(new ReflectionSourceSnapshot.DailyReflectionSource(
                        saved.id(), date, saved.title(), "saved", true,
                        "saved_reflection", content
                ));

                evidence.add(new ReflectionSourceSnapshot.Evidence(
                        "reflection:" + saved.id(),
                        saved.title(),
                        date + " · 저장된 일일 회고",
                        content
                ));
                continue;
            }

            missingDates.add(date);
            List<Activity> fallback = activitiesByDate.getOrDefault(date, List.of());
            fallbackActivityCount += fallback.size();

            dailySources.add(new ReflectionSourceSnapshot.DailyReflectionSource(
                    null, date, null, "missing", false,
                    fallback.isEmpty() ? "missing_source" : "fallback_activity", null
            ));
            fallback.forEach(activity -> evidence.add(activityEvidence(activity)));
        }

        // 회고 기간 내의 개발 로그를 evidence에 추가합니다.
        devlogs.forEach(devlog -> evidence.add(devlogEvidence(devlog)));

        return new ReflectionSourceSnapshot(
                Math.toIntExact(activityBatch.totalCount()),
                devlogs.size(),
                evidence,
                Math.toIntExact(periodStart.datesUntil(periodEnd.plusDays(1)).count()),
                savedByDate.size(),
                missingDates,
                fallbackActivityCount,
                dailySources,
                collectionGap
        );
    }

    /**
     * 회고 소스 스냅샷의 품질을 평가합니다.
     * - 일일 회고의 경우, collectionGap이 없고, 예상 일일 회고 수와 저장된 일일 회고 수가 동일하면 COMPLETE, 그렇지 않으면 PARTIAL로 평가합니다.
     * - 주간 회고의 경우, collectionGap이 있으면 PARTIAL, 그렇지 않으면 COMPLETE로 평가합니다.
     *
     * @param kind 회고 종류 (일일/주간)
     * @param snapshot 회고 소스 스냅샷
     * @return 평가된 SourceQuality
     */
    private SourceQuality quality(
            ReflectionKind kind, ReflectionSourceSnapshot snapshot
    ) {
        if (kind == ReflectionKind.WEEKLY) {
            return !snapshot.collectionGap()
                    && snapshot.expectedDailyCount().equals(snapshot.savedDailyCount())
                    ? SourceQuality.COMPLETE : SourceQuality.PARTIAL;
        }
        return snapshot.collectionGap() ? SourceQuality.PARTIAL : SourceQuality.COMPLETE;
    }

    /**
     * 활동(Activity) 엔티티를 기반으로 Evidence 객체를 생성합니다.
     * - Evidence의 ref는 "activity:{activityId}" 형식으로 생성됩니다.
     * - Evidence의 title은 활동의 제목 또는 활동 타입의 데이터베이스 값을 사용합니다.
     * - Evidence의 label은 브랜치 이름과 커밋 SHA의 짧은 버전을 결합하여 생성됩니다.
     * - Evidence의 content는 활동 요약 또는 제목을 사용합니다.
     *
     * @param activity 활동 엔티티
     * @return 생성된 Evidence 객체
     */
    private ReflectionSourceSnapshot.Evidence activityEvidence(Activity activity) {
        String title = textOr(activity.title(), activity.type().getDatabaseValue());
        String sha = activity.commitSha();
        String shortSha = sha == null ? null : sha.substring(0, Math.min(7, sha.length()));
        String label = Stream.of(activity.branchName(), shortSha)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" · "));
        return new ReflectionSourceSnapshot.Evidence(
                "activity:" + activity.id(),
                title,
                label,
                textOr(activity.summary(), title)
        );
    }

    private ReflectionSourceSnapshot.Evidence devlogEvidence(Devlog devlog) {
        return new ReflectionSourceSnapshot.Evidence(
                "devlog:" + devlog.id(),
                devlog.logDate() + " 개발로그",
                "개발로그",
                devlog.contentMarkdown()
        );
    }

    /**
     * Evidence와 일일 회고 근거를 정렬해 순서 변화에 영향받지 않는 SHA-256 hash 생성
     * 근거 내용 변경 여부 및 동일 근거 재생성 차단에 사용하는 16진수 지문 반환
     */
    private String hash(ReflectionSourceSnapshot snapshot) {
        List<String> values = new ArrayList<>();
        snapshot.evidence().stream()
                .sorted(Comparator.comparing(ReflectionSourceSnapshot.Evidence::ref))
                .forEach(evidence -> values.add(
                        evidence.ref() + "|" + evidence.title() + "|"
                                + evidence.label() + "|" + evidence.content()
                ));
        snapshot.dailyReflections().stream()
                .sorted(Comparator.comparing(ReflectionSourceSnapshot.DailyReflectionSource::date))
                .forEach(daily -> values.add(
                        daily.date() + "|" + daily.reflectionId() + "|" + daily.status()
                                + "|" + daily.reason() + "|" + daily.content()
                ));
        values.add("gap=" + snapshot.collectionGap());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", values).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("source hash calculation failed", exception);
        }
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record Result(
            ReflectionSourceSnapshot snapshot,
            SourceQuality quality,
            String sourceHash
    ) {
        public boolean hasSource() {
            return snapshot.hasSource();
        }
    }
}
