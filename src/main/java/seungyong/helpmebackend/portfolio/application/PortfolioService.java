package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioPortIn;
import seungyong.helpmebackend.portfolio.application.port.in.command.CreatePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.in.command.GetPortfolioSourcesQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.ListPortfoliosQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.RegeneratePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.in.command.SavePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioSourcePortOut;
import seungyong.helpmebackend.portfolio.application.port.out.result.PortfolioCreateResult;
import seungyong.helpmebackend.portfolio.domain.entity.*;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioGenerationMode;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PortfolioService implements PortfolioPortIn {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int RETRY_AFTER_SECONDS = 2;

    private final ProjectAccessResolver projectAccessResolver;
    private final PortfolioPortOut portfolioPortOut;
    private final PortfolioSourcePortOut sourcePortOut;
    private final PortfolioSourceBuilder sourceBuilder;

    @Override
    public PortfolioSourceCatalog getSources(GetPortfolioSourcesQuery query) {
        validateSourcesQuery(query);

        Project project = projectAccessResolver.resolveActive(query.userId(), query.projectId());

        ZoneId zoneId = ZoneId.of(project.getSettings().timezone());
        PortfolioSourceData data = sourcePortOut.findCandidates(
                project.getId(), query.periodStart(), query.periodEnd(),
                query.periodStart().atStartOfDay(zoneId).toOffsetDateTime(),
                query.periodEnd().plusDays(1).atStartOfDay(zoneId).toOffsetDateTime()
        );

        List<PortfolioSourceCatalog.ReflectionCandidate> reflections = data.reflections().stream()
                .map(source -> new PortfolioSourceCatalog.ReflectionCandidate(
                        source.id(), source.kind(), source.periodStart(), source.periodEnd(),
                        source.title(), source.version(), true
                )).toList();
        List<PortfolioSourceCatalog.ActivityCandidate> activities = data.activities().stream()
                .map(source -> {
                    boolean selectable = !project.isPrivateRepository() && StringUtils.hasText(source.publicUrl());
                    return new PortfolioSourceCatalog.ActivityCandidate(
                            source.id(), apiActivityType(source), source.title(), activityLabel(source),
                            selectable ? source.publicUrl() : null, selectable,
                            selectable ? null : project.isPrivateRepository() ? "private_repository" : "public_url_unavailable"
                    );
                }).toList();

        return new PortfolioSourceCatalog(
                PortfolioEligibility.from(reflections.size()), reflections, activities,
                new PortfolioSourceCatalog.Defaults(query.periodStart(), query.periodEnd(), PortfolioTone.CONCISE)
        );
    }

    @Override
    public PortfolioPage getPortfolios(ListPortfoliosQuery query) {
        if (query == null || query.userId() == null || query.projectId() == null) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }

        Project project = projectAccessResolver.resolveActive(query.userId(), query.projectId());
        PortfolioStatus status = parseStatus(query.status());
        int size = normalizeSize(query.size());

        PortfolioCursor cursor = decodeCursor(query.cursor());
        List<Portfolio> found = portfolioPortOut.findPage(
                project.getId(), status, cursor.updatedAt(), cursor.id(), size + 1
        );

        boolean hasNext = found.size() > size;
        List<Portfolio> page = hasNext ? new ArrayList<>(found.subList(0, size)) : found;

        Map<Long, PortfolioLastExportSummary> latestExports =
                portfolioPortOut.findLatestExportSummaries(page.stream().map(Portfolio::id).toList());
        List<PortfolioPage.Item> items = page.stream().map(portfolio -> new PortfolioPage.Item(
                portfolio.id(), portfolio.title(), portfolio.periodStart(), portfolio.periodEnd(),
                portfolio.tone(), portfolio.status(), portfolio.content().sections().size(),
                portfolio.sourceSnapshot().reflections().size(),
                portfolio.sourceSnapshot().activities().size() + portfolio.sourceSnapshot().customLinks().size(),
                portfolio.version(), portfolio.updatedAt(), latestExports.get(portfolio.id()), portfolio.error()
        )).toList();

        return new PortfolioPage(
                items,
                PortfolioEligibility.from(sourcePortOut.countSavedReflections(project.getId())),
                hasNext ? encodeCursor(page.get(page.size() - 1)) : null,
                hasNext
        );
    }

    @Override
    public Portfolio getPortfolio(Long userId, Long projectId, Long portfolioId) {
        validateIdentity(userId, projectId, portfolioId);
        projectAccessResolver.resolveActive(userId, projectId);

        Portfolio portfolio = findPortfolio(projectId, portfolioId);
        boolean changed = !sourcePortOut.reflectionVersionsMatch(
                projectId, portfolio.sourceSnapshot().reflections()
        );

        return copyWithSourceChanged(portfolio, changed);
    }

    @Override
    public PortfolioGenerationResult createPortfolio(CreatePortfolioCommand command) {
        validateCreate(command);

        Project project = projectAccessResolver.resolveActive(command.userId(), command.projectId());
        Portfolio existing = portfolioPortOut.getByProjectIdAndRequestKey(
                project.getId(), command.idempotencyKey()
        ).orElse(null);

        if (existing != null) {
            return generationResult(existing, false);
        }

        PortfolioTone tone = parseTone(command.tone());
        PortfolioGenerationMode mode = parseMode(command.generationMode());

        PortfolioSourceBuildResult source = sourceBuilder.build(
                project, command.reflectionIds(), command.activityIds(), command.customEvidenceLinks()
        );

        // 회고 기간이 포트폴리오 기간을 벗어나면 생성 불가
        boolean outsidePeriod = source.snapshot().reflections().stream().anyMatch(reflection ->
                reflection.periodStart().isBefore(command.periodStart())
                        || reflection.periodEnd().isAfter(command.periodEnd())
        );
        if (outsidePeriod) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_SOURCE_REQUIRED);
        }

        Portfolio portfolio = Portfolio.builder()
                .projectId(project.getId())
                .requestKey(command.idempotencyKey())
                .title(command.title().trim())
                .periodStart(command.periodStart())
                .periodEnd(command.periodEnd())
                .tone(tone)
                .status(mode == PortfolioGenerationMode.AI ? PortfolioStatus.QUEUED : PortfolioStatus.DRAFT)
                .content(PortfolioDocument.empty())
                // 비동기 워커가 선택 당시의 회고 version과 공개 근거만 사용하도록 snapshot/hash 저장
                .sourceSnapshot(source.snapshot())
                .sourceHash(source.sourceHash())
                .generationAttempts((short) 0)
                .version(0)
                .build();
        PortfolioCreateResult created = portfolioPortOut.createIfAbsent(portfolio);

        return generationResult(created.portfolio(), created.created());
    }

    @Override
    public Portfolio savePortfolio(SavePortfolioCommand command) {
        validateSave(command);

        Project project = projectAccessResolver.resolveActive(command.userId(), command.projectId());

        Portfolio current = findPortfolio(project.getId(), command.portfolioId());
        if (current.version() != command.version()) {
            throw new CustomException(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
        }

        validateDocumentEvidence(project, current.sourceSnapshot(), command.content());

        return portfolioPortOut.saveIfVersionMatches(
                project.getId(), command.portfolioId(), command.title().trim(), parseTone(command.tone()),
                command.content(), command.version(), OffsetDateTime.now(ZoneOffset.UTC)
        ).orElseThrow(() -> new CustomException(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT));
    }

    @Override
    public PortfolioGenerationResult regeneratePortfolio(RegeneratePortfolioCommand command) {
        validateIdentity(command.userId(), command.projectId(), command.portfolioId());

        Project project = projectAccessResolver.resolveActive(command.userId(), command.projectId());

        Portfolio current = findPortfolio(project.getId(), command.portfolioId());
        if (current.isGenerating()) {
            return generationResult(current, false);
        }

        PortfolioSourceBuildResult source = command.refreshSources()
                ? sourceBuilder.refresh(project, current.sourceSnapshot())
                : new PortfolioSourceBuildResult(current.sourceSnapshot(), current.sourceHash());

        Portfolio queued = portfolioPortOut.queueRegeneration(
                project.getId(), current.id(), source.snapshot(), source.sourceHash()
        ).orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        return generationResult(queued, false);
    }

    private Portfolio findPortfolio(Long projectId, Long portfolioId) {
        return portfolioPortOut.getByProjectIdAndId(projectId, portfolioId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private void validateDocumentEvidence(Project project, PortfolioSourceSnapshot source, PortfolioDocument document) {
        Set<String> allowed = new HashSet<>();
        source.reflections().forEach(item -> allowed.add("reflection:" + item.id()));
        source.activities().forEach(item -> allowed.add("activity:" + item.id()));

        for (int index = 0; index < source.customLinks().size(); index++) {
            allowed.add("custom_link:" + index);
        }

        // 문서에 포함된 근거 중 허용되지 않은 근거가 있는지 확인
        boolean unknown = document.sections().stream().flatMap(section -> section.evidenceRefs().stream())
                .anyMatch(ref -> !allowed.contains(ref));

        // 문서에 비공개 또는 서명 근거가 포함되어 있는지 확인
        String repositoryUrl = "github.com/" + project.getRepoFullName().toLowerCase();
        boolean unsafeLink = document.sections().stream().map(PortfolioDocument.Section::contentMd)
                .map(String::toLowerCase)
                .anyMatch(content -> content.contains("token=") || content.contains("signature=")
                        || content.contains("x-amz-")
                        || (project.isPrivateRepository()
                        && (content.contains(repositoryUrl + "/") || content.endsWith(repositoryUrl))));

        // 만약 허용되지 않은 근거가 포함되어 있거나, 비공개/서명 근거가 포함되어 있다면 예외를 발생시킴
        if (unknown || unsafeLink) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
        }
    }

    private Portfolio copyWithSourceChanged(Portfolio source, boolean changed) {
        return Portfolio.builder().id(source.id()).projectId(source.projectId()).requestKey(source.requestKey())
                .title(source.title()).periodStart(source.periodStart()).periodEnd(source.periodEnd())
                .tone(source.tone()).status(source.status()).content(source.content())
                .sourceSnapshot(source.sourceSnapshot()).sourceHash(source.sourceHash())
                .generationAttempts(source.generationAttempts()).generationStartedAt(source.generationStartedAt())
                .generatedAt(source.generatedAt()).savedAt(source.savedAt()).error(source.error())
                .version(source.version()).createdAt(source.createdAt()).updatedAt(source.updatedAt())
                .sourceChanged(changed).build();
    }

    private PortfolioGenerationResult generationResult(Portfolio portfolio, boolean created) {
        return new PortfolioGenerationResult(
                portfolio.id(), portfolio.status(), portfolio.version(), created,
                portfolio.isGenerating(), portfolio.isGenerating() ? RETRY_AFTER_SECONDS : 0
        );
    }

    private void validateSourcesQuery(GetPortfolioSourcesQuery query) {
        if (query == null || query.userId() == null || query.projectId() == null
                || query.periodStart() == null || query.periodEnd() == null
                || query.periodStart().isAfter(query.periodEnd())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateCreate(CreatePortfolioCommand command) {
        if (command == null || command.userId() == null || command.projectId() == null
                || command.idempotencyKey() == null || !StringUtils.hasText(command.title())
                || command.periodStart() == null || command.periodEnd() == null
                || command.periodStart().isAfter(command.periodEnd())
                || command.reflectionIds() == null || command.reflectionIds().stream().anyMatch(java.util.Objects::isNull)
                || command.activityIds() == null || command.activityIds().stream().anyMatch(java.util.Objects::isNull)
                || command.customEvidenceLinks() == null
                || command.customEvidenceLinks().stream().anyMatch(java.util.Objects::isNull)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateSave(SavePortfolioCommand command) {
        if (command == null || !StringUtils.hasText(command.title()) || command.content() == null
                || command.version() == null || command.version() < 0) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        validateIdentity(command.userId(), command.projectId(), command.portfolioId());
    }

    private void validateIdentity(Long userId, Long projectId, Long portfolioId) {
        if (userId == null || projectId == null || portfolioId == null) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private PortfolioStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) return null;

        try {
            return PortfolioStatus.fromDatabaseValue(value.trim());
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private PortfolioTone parseTone(String value) {
        try {
            return PortfolioTone.fromDatabaseValue(value == null ? null : value.trim());
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private PortfolioGenerationMode parseMode(String value) {
        try {
            return PortfolioGenerationMode.fromApiValue(StringUtils.hasText(value) ? value.trim() : "ai");
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private int normalizeSize(Integer size) {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < 1 || value > MAX_SIZE) throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        return value;
    }

    private PortfolioCursor decodeCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) return new PortfolioCursor(null, null);
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            return new PortfolioCursor(OffsetDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private String encodeCursor(Portfolio portfolio) {
        String raw = portfolio.updatedAt() + "|" + portfolio.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String apiActivityType(PortfolioSourceData.ActivityData activity) {
        return activity.type() == seungyong.helpmebackend.activity.domain.type.ActivityType.PUSH_COMMIT
                ? "commit" : "pull_request";
    }

    private String activityLabel(PortfolioSourceData.ActivityData activity) {
        String branch = StringUtils.hasText(activity.branchName()) ? activity.branchName() : "activity";
        if (!StringUtils.hasText(activity.commitSha())) return branch;
        return branch + " · " + activity.commitSha().substring(0, Math.min(7, activity.commitSha().length()));
    }
}
