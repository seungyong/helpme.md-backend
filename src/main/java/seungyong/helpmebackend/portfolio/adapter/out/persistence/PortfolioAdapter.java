package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.result.PortfolioCreateResult;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioLastExportSummary;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PortfolioAdapter implements PortfolioPortOut {
    private static final int MAX_ATTEMPTS = 3;

    private final PortfolioJpaRepository portfolioJpaRepository;
    private final PortfolioExportQueryJpaRepository portfolioExportQueryJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PortfolioCreateResult createIfAbsent(Portfolio portfolio) {
        Optional<PortfolioJpaEntity> existing = portfolioJpaRepository
                .findByProject_IdAndRequestKey(portfolio.projectId(), portfolio.requestKey());
        if (existing.isPresent()) {
            return new PortfolioCreateResult(toDomain(existing.orElseThrow()), false);
        }
        try {
            PortfolioJpaEntity saved = portfolioJpaRepository.saveAndFlush(toJpaEntity(portfolio));
            return new PortfolioCreateResult(toDomain(saved), true);
        } catch (DataIntegrityViolationException exception) {
            PortfolioJpaEntity duplicate = portfolioJpaRepository
                    .findByProject_IdAndRequestKey(portfolio.projectId(), portfolio.requestKey())
                    .orElseThrow(() -> exception);
            return new PortfolioCreateResult(toDomain(duplicate), false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Portfolio> getByProjectIdAndId(Long projectId, Long portfolioId) {
        return portfolioJpaRepository.findByProject_IdAndId(projectId, portfolioId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Portfolio> getByProjectIdAndRequestKey(Long projectId, java.util.UUID requestKey) {
        return portfolioJpaRepository.findByProject_IdAndRequestKey(projectId, requestKey).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Portfolio> findPage(Long projectId, PortfolioStatus status, OffsetDateTime cursorUpdatedAt,
                                    Long cursorId, int limit) {
        return portfolioJpaRepository.findPage(
                projectId, status, cursorUpdatedAt, cursorId, PageRequest.of(0, limit)
        ).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PortfolioLastExportSummary> findLatestExportSummaries(List<Long> portfolioIds) {
        if (portfolioIds.isEmpty()) {
            return Map.of();
        }
        return portfolioExportQueryJpaRepository.findLatestByPortfolioIds(portfolioIds).stream()
                .collect(Collectors.toUnmodifiableMap(
                        entity -> entity.getPortfolio().getId(),
                        entity -> new PortfolioLastExportSummary(
                                entity.getFormat(), entity.getStatus(), entity.getCompletedAt()
                        )
                ));
    }

    @Override
    @Transactional
    public Optional<Portfolio> saveIfVersionMatches(Long projectId, Long portfolioId, String title,
                                                     PortfolioTone tone, PortfolioDocument content,
                                                     int expectedVersion, OffsetDateTime savedAt) {
        Optional<PortfolioJpaEntity> locked = portfolioJpaRepository.findForUpdate(projectId, portfolioId);
        if (locked.isEmpty() || locked.orElseThrow().getVersion() != expectedVersion) {
            return Optional.empty();
        }
        PortfolioJpaEntity entity = locked.orElseThrow();
        entity.saveDocument(title, tone, toJson(content), savedAt);
        portfolioJpaRepository.flush();
        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public Optional<Portfolio> queueRegeneration(Long projectId, Long portfolioId,
                                                  PortfolioSourceSnapshot snapshot, String sourceHash) {
        Optional<PortfolioJpaEntity> locked = portfolioJpaRepository.findForUpdate(projectId, portfolioId);
        if (locked.isEmpty()) {
            return Optional.empty();
        }

        PortfolioJpaEntity entity = locked.orElseThrow();
        if (entity.getStatus() != PortfolioStatus.QUEUED && entity.getStatus() != PortfolioStatus.GENERATING) {
            entity.queue(toJson(snapshot), sourceHash);
            portfolioJpaRepository.flush();
        }

        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public Optional<Portfolio> claimNext(OffsetDateTime now, OffsetDateTime stuckBefore) {
        for (PortfolioJpaEntity stuck : portfolioJpaRepository.findStuck(PortfolioStatus.GENERATING, stuckBefore)) {
            if (stuck.getGenerationAttempts() >= MAX_ATTEMPTS) {
                stuck.failGeneration("PORTFOLIO_50001", "중단된 포트폴리오 생성 작업이 최대 재시도 횟수를 초과했습니다.");
            } else {
                stuck.requeueStuck();
            }
        }
        List<PortfolioJpaEntity> claimable = portfolioJpaRepository.findClaimable(
                PortfolioStatus.QUEUED, PageRequest.of(0, 1)
        );
        if (claimable.isEmpty()) {
            return Optional.empty();
        }
        PortfolioJpaEntity entity = claimable.get(0);
        entity.claim(now);
        portfolioJpaRepository.flush();
        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public void completeGeneration(Long portfolioId, PortfolioDocument content, OffsetDateTime generatedAt) {
        portfolioJpaRepository.findById(portfolioId).orElseThrow()
                .completeGeneration(toJson(content), generatedAt);
    }

    @Override
    @Transactional
    public void failGeneration(Long portfolioId, String errorCode, String errorMessage) {
        portfolioJpaRepository.findById(portfolioId).orElseThrow().failGeneration(errorCode, errorMessage);
    }

    private PortfolioJpaEntity toJpaEntity(Portfolio portfolio) {
        return PortfolioJpaEntity.builder()
                .id(portfolio.id())
                .project(ProjectJpaEntity.builder().id(portfolio.projectId()).build())
                .requestKey(portfolio.requestKey())
                .title(portfolio.title())
                .periodStart(portfolio.periodStart())
                .periodEnd(portfolio.periodEnd())
                .tone(portfolio.tone())
                .status(portfolio.status())
                .content(toJson(portfolio.content()))
                .sourceSnapshot(toJson(portfolio.sourceSnapshot()))
                .sourceHash(portfolio.sourceHash())
                .generationAttempts(portfolio.generationAttempts())
                .generationStartedAt(portfolio.generationStartedAt())
                .generatedAt(portfolio.generatedAt())
                .savedAt(portfolio.savedAt())
                .errorCode(portfolio.error() == null ? null : portfolio.error().code())
                .errorMessage(portfolio.error() == null ? null : portfolio.error().message())
                .version(portfolio.version())
                .createdAt(portfolio.createdAt())
                .updatedAt(portfolio.updatedAt())
                .build();
    }

    private Portfolio toDomain(PortfolioJpaEntity entity) {
        return Portfolio.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .requestKey(entity.getRequestKey())
                .title(entity.getTitle())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .tone(entity.getTone())
                .status(entity.getStatus())
                .content(readDocument(entity.getContent()))
                .sourceSnapshot(read(entity.getSourceSnapshot(), PortfolioSourceSnapshot.class))
                .sourceHash(entity.getSourceHash())
                .generationAttempts(entity.getGenerationAttempts())
                .generationStartedAt(entity.getGenerationStartedAt())
                .generatedAt(entity.getGeneratedAt())
                .savedAt(entity.getSavedAt())
                .error(entity.getErrorCode() == null ? null
                        : new Portfolio.PortfolioError(entity.getErrorCode(), entity.getErrorMessage(), true))
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PortfolioDocument readDocument(JsonNode node) {
        if (node != null && node.has("schemaVersion")) {
            return read(node, PortfolioDocument.class);
        }
        List<PortfolioDocument.Section> sections = new ArrayList<>();
        if (node != null && node.path("sections").isArray()) {
            node.path("sections").forEach(section -> sections.add(read(section, PortfolioDocument.Section.class)));
        }
        return new PortfolioDocument(PortfolioDocument.CURRENT_SCHEMA_VERSION, sections);
    }

    private JsonNode toJson(Object value) {
        return objectMapper.valueToTree(value);
    }

    private <T> T read(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (Exception exception) {
            throw new IllegalStateException("invalid portfolio json", exception);
        }
    }
}
