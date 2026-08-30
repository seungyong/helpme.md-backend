package seungyong.helpmebackend.reflection.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.reflection.application.port.in.ReflectionPortIn;
import seungyong.helpmebackend.reflection.application.port.in.command.CreateReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.in.command.ListReflectionsQuery;
import seungyong.helpmebackend.reflection.application.port.in.command.RegenerateReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.in.command.SaveReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionGenerationResult;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionPage;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionErrorCode;
import seungyong.helpmebackend.reflection.domain.type.ReflectionGenerationMode;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReflectionService implements ReflectionPortIn {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int RETRY_AFTER_SECONDS = 2;

    private final ProjectAccessResolver projectAccessResolver;
    private final ReflectionPortOut reflectionPortOut;
    private final ReflectionSourceBuilder sourceBuilder;

    @Override
    public ReflectionPage getReflections(ListReflectionsQuery query) {
        validateListQuery(query);

        Project project = projectAccessResolver.resolveActive(query.userId(), query.projectId());
        ReflectionKind kind = parseKind(query.kind());
        ReflectionStatus status = parseStatus(query.status());
        int size = normalizeSize(query.size());
        Cursor cursor = decodeCursor(query.cursor());

        // size + 1개를 조회하여 hasNext 여부를 판단
        List<Reflection> found = reflectionPortOut.findPage(
                project.getId(), kind, query.from(), query.to(), status,
                cursor.periodStart(), cursor.id(), size + 1
        );
        boolean hasNext = found.size() > size;

        // hasNext가 true이면 found에서 size만큼 잘라서 items에 넣고, 마지막 아이템의 커서를 nextCursor로 설정
        List<Reflection> items = hasNext
                ? new ArrayList<>(found.subList(0, size)) : found;
        String nextCursor = hasNext ? encodeCursor(items.get(items.size() - 1)) : null;

        return new ReflectionPage(
                items,
                currentPeriod(project, kind),
                nextCursor,
                hasNext
        );
    }

    @Override
    public Reflection getReflection(Long userId, Long projectId, Long reflectionId) {
        validateIdentity(userId, projectId, reflectionId);
        projectAccessResolver.resolveActive(userId, projectId);

        return reflectionPortOut.getByProjectIdAndId(projectId, reflectionId)
                .orElseThrow(() -> new CustomException(
                        ReflectionErrorCode.REFLECTION_NOT_FOUND
                ));
    }

    @Override
    public ReflectionGenerationResult createReflection(CreateReflectionCommand command) {
        validateCreateCommand(command);
        Project project = projectAccessResolver.resolveActive(
                command.userId(), command.projectId()
        );

        ReflectionKind kind = parseKind(command.kind());
        ReflectionGenerationMode mode = parseMode(command.generationMode());
        boolean allowPartial = command.allowPartial() == null || command.allowPartial();

        Reflection existing = reflectionPortOut.getByPeriod(
                project.getId(), kind, command.periodStart()
        ).orElse(null);

        // Reflection이 있다면, 새로운 Reflection을 생성하지 않고 기존 Reflection을 반환
        if (existing != null) {
            return generationResult(existing, false);
        }

        LocalDate periodEnd = kind == ReflectionKind.DAILY
                ? command.periodStart() : command.periodStart().plusDays(6);

        // 생성 요청 시점의 AI 근거 snapshot과 동일 근거 판별용 hash 생성
        ReflectionSourceBuilder.Result source = sourceBuilder.build(
                project, kind, command.periodStart(), periodEnd
        );

        // AI 모드일 경우, 소스 데이터가 충분한지 확인하고, 부족하면 예외를 발생시킴
        if (mode == ReflectionGenerationMode.AI) {
            requireUsableSource(source, allowPartial);
        }

        Reflection reflection = Reflection.builder()
                .projectId(project.getId())
                .kind(kind)
                .periodStart(command.periodStart())
                .periodEnd(periodEnd)
                .title(mode == ReflectionGenerationMode.BLANK
                        ? defaultTitle(kind, command.periodStart(), periodEnd) : null)
                .content(ReflectionDocument.empty())
                .status(mode == ReflectionGenerationMode.BLANK
                        ? ReflectionStatus.DRAFT : ReflectionStatus.QUEUED)
                // 워커 실행 전 작업이 유실되지 않도록 요청 시점 snapshot/hash 선저장
                .sourceQuality(source.quality())
                .sourceSnapshot(source.snapshot())
                .sourceHash(source.sourceHash())
                .generationAttempts((short) 0)
                .version(0)
                .build();
        ReflectionPortOut.CreateResult created = reflectionPortOut.createIfAbsent(reflection);
        return generationResult(created.reflection(), created.created());
    }

    @Override
    public Reflection saveReflection(SaveReflectionCommand command) {
        validateSaveCommand(command);
        projectAccessResolver.resolveActive(command.userId(), command.projectId());

        Reflection current = reflectionPortOut.getByProjectIdAndId(
                command.projectId(), command.reflectionId()
        ).orElseThrow(() -> new CustomException(ReflectionErrorCode.REFLECTION_NOT_FOUND));

        if (current.version() != command.version()) {
            throw new CustomException(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
        }

        return reflectionPortOut.saveIfVersionMatches(
                command.projectId(),
                command.reflectionId(),
                command.title().trim(),
                command.content(),
                command.version(),
                OffsetDateTime.now(ZoneOffset.UTC)
        ).orElseThrow(() -> new CustomException(
                DocumentErrorCode.DOCUMENT_VERSION_CONFLICT
        ));
    }

    @Override
    public ReflectionGenerationResult regenerateReflection(
            RegenerateReflectionCommand command
    ) {
        validateIdentity(command == null ? null : command.userId(),
                command == null ? null : command.projectId(),
                command == null ? null : command.reflectionId());

        Project project = projectAccessResolver.resolveActive(
                command.userId(), command.projectId()
        );
        Reflection current = reflectionPortOut.getByProjectIdAndId(
                command.projectId(), command.reflectionId()
        ).orElseThrow(() -> new CustomException(ReflectionErrorCode.REFLECTION_NOT_FOUND));

        if (current.isGenerating()) {
            return generationResult(current, false);
        }

        ReflectionSourceBuilder.Result source = sourceBuilder.build(
                project, current.kind(), current.periodStart(), current.periodEnd()
        );

        requireUsableSource(source, command.allowPartial() == null || command.allowPartial());

        // 마지막 AI 성공 근거와 최신 hash가 같으면 queue 생략, 동일 근거의 GPT 토큰 재소비 차단
        if (hasSameSuccessfullyGeneratedSource(current, source)) {
            return generationResult(current, false);
        }

        Reflection queued = reflectionPortOut.queueRegeneration(
                project.getId(), current.id()
        ).orElseThrow(() -> new CustomException(ReflectionErrorCode.REFLECTION_NOT_FOUND));

        return generationResult(queued, false);
    }

    private boolean hasSameSuccessfullyGeneratedSource(
            Reflection current, ReflectionSourceBuilder.Result source
    ) {
        boolean successfullyGenerated =
                current.status() == ReflectionStatus.DRAFT
                        || current.status() == ReflectionStatus.SAVED;
        return successfullyGenerated
                && current.generatedAt() != null
                && current.sourceHash() != null
                && current.sourceHash().equals(source.sourceHash());
    }

    private ReflectionPage.CurrentPeriod currentPeriod(
            Project project, ReflectionKind kind
    ) {
        ZoneId zoneId = ZoneId.of(project.getSettings().timezone());
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        Period period = kind == ReflectionKind.DAILY
                ? dailyPeriod(project, now) : weeklyPeriod(project, now);

        Reflection existing = reflectionPortOut.getByPeriod(
                project.getId(), kind, period.start()
        ).orElse(null);
        if (existing != null) {
            return new ReflectionPage.CurrentPeriod(
                    true, existing.id(), kind, period.start(), period.end(),
                    existing.status(), existing.sourceQuality(), existing.version(),
                    existing.error(), false, "already_exists", period.scheduledAt()
            );
        }

        // 현재 시각이 scheduledAt보다 이전이면, 아직 회고를 생성할 수 없는 상태이므로, canGenerate=false로 반환
        if (now.toOffsetDateTime().isBefore(period.scheduledAt())) {
            return new ReflectionPage.CurrentPeriod(
                    false, null, kind, period.start(), period.end(),
                    null, null, null, null, false,
                    "period_in_progress", period.scheduledAt()
            );
        }

        ReflectionSourceBuilder.Result source = sourceBuilder.build(
                project, kind, period.start(), period.end()
        );
        boolean canGenerate = source.hasSource();
        return new ReflectionPage.CurrentPeriod(
                false, null, kind, period.start(), period.end(),
                null, null, null, null, canGenerate,
                canGenerate ? null : "insufficient_source", period.scheduledAt()
        );
    }

    private Period dailyPeriod(Project project, ZonedDateTime now) {
        LocalDate date = now.toLocalDate();
        OffsetDateTime scheduledAt = date.atTime(
                project.getSettings().daily().generationTime()
        ).atZone(now.getZone()).toOffsetDateTime();
        return new Period(date, date, scheduledAt);
    }

    private Period weeklyPeriod(Project project, ZonedDateTime now) {
        ReflectionWeekday target = project.getSettings().weekly().generationDay();
        DayOfWeek targetDay = target.getDatabaseValue() == 0
                ? DayOfWeek.SUNDAY : DayOfWeek.of(target.getDatabaseValue());
        LocalDate end = now.toLocalDate().with(TemporalAdjusters.nextOrSame(targetDay));
        OffsetDateTime scheduledAt = end.atTime(
                project.getSettings().weekly().generationTime()
        ).atZone(now.getZone()).toOffsetDateTime();
        if (end.equals(now.toLocalDate()) && now.toOffsetDateTime().isAfter(scheduledAt)) {
            return new Period(end.minusDays(6), end, scheduledAt);
        }
        return new Period(end.minusDays(6), end, scheduledAt);
    }

    private void requireUsableSource(
            ReflectionSourceBuilder.Result source, boolean allowPartial
    ) {
        if (!source.hasSource()
                || (!allowPartial && source.quality() == SourceQuality.PARTIAL)) {
            throw new CustomException(ReflectionErrorCode.REFLECTION_SOURCE_INSUFFICIENT);
        }
    }

    private ReflectionGenerationResult generationResult(
            Reflection reflection, boolean created
    ) {
        return new ReflectionGenerationResult(
                reflection.id(),
                reflection.status(),
                created,
                reflection.isGenerating(),
                reflection.isGenerating() ? RETRY_AFTER_SECONDS : 0
        );
    }

    private String defaultTitle(
            ReflectionKind kind, LocalDate start, LocalDate end
    ) {
        return kind == ReflectionKind.DAILY
                ? start + " 일일 회고" : start + " ~ " + end + " 주간 회고";
    }

    private void validateListQuery(ListReflectionsQuery query) {
        if (query == null || query.userId() == null || query.projectId() == null
                || !StringUtils.hasText(query.kind())
                || (query.from() != null && query.to() != null
                && query.from().isAfter(query.to()))) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateCreateCommand(CreateReflectionCommand command) {
        if (command == null || command.userId() == null || command.projectId() == null
                || !StringUtils.hasText(command.kind()) || command.periodStart() == null) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateSaveCommand(SaveReflectionCommand command) {
        if (command == null || !StringUtils.hasText(command.title())
                || command.content() == null || command.version() == null
                || command.version() < 0) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        validateIdentity(command.userId(), command.projectId(), command.reflectionId());
    }

    private void validateIdentity(Long userId, Long projectId, Long reflectionId) {
        if (userId == null || projectId == null || reflectionId == null) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private ReflectionKind parseKind(String value) {
        try {
            return ReflectionKind.fromDatabaseValue(value == null ? null : value.trim());
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private ReflectionStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ReflectionStatus.fromDatabaseValue(value.trim());
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private ReflectionGenerationMode parseMode(String value) {
        if (!StringUtils.hasText(value)) {
            return ReflectionGenerationMode.BLANK;
        }
        try {
            return ReflectionGenerationMode.fromApiValue(value.trim());
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private int normalizeSize(Integer size) {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < 1 || value > MAX_SIZE) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        return value;
    }

    private Cursor decodeCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return new Cursor(null, null);
        }
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8
            );
            String[] parts = raw.split("\\|", 2);
            return new Cursor(LocalDate.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private String encodeCursor(Reflection reflection) {
        String raw = reflection.periodStart() + "|" + reflection.id();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private record Cursor(LocalDate periodStart, Long id) {
    }

    private record Period(
            LocalDate start, LocalDate end, OffsetDateTime scheduledAt
    ) {
    }
}
