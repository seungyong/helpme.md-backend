package seungyong.helpmebackend.reflection.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.reflection.adapter.out.persistence.entity.ReflectionJpaEntity;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReflectionAdapter implements ReflectionPortOut {
    private static final int MAX_ATTEMPTS = 3;

    private final ReflectionJpaRepository reflectionJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CreateResult createIfAbsent(Reflection reflection) {
        Optional<ReflectionJpaEntity> existing = reflectionJpaRepository
                .findByProject_IdAndKindAndPeriodStart(
                        reflection.projectId(), reflection.kind(), reflection.periodStart()
                );
        if (existing.isPresent()) {
            return new CreateResult(toDomain(existing.orElseThrow()), false);
        }
        try {
            ReflectionJpaEntity saved = reflectionJpaRepository.saveAndFlush(toJpaEntity(reflection));
            return new CreateResult(toDomain(saved), true);
        } catch (DataIntegrityViolationException exception) {
            ReflectionJpaEntity duplicate = reflectionJpaRepository
                    .findByProject_IdAndKindAndPeriodStart(
                            reflection.projectId(), reflection.kind(), reflection.periodStart()
                    )
                    .orElseThrow(() -> exception);
            return new CreateResult(toDomain(duplicate), false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reflection> getByProjectIdAndId(Long projectId, Long reflectionId) {
        return reflectionJpaRepository.findByProject_IdAndId(projectId, reflectionId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reflection> getByPeriod(
            Long projectId, ReflectionKind kind, LocalDate periodStart
    ) {
        return reflectionJpaRepository
                .findByProject_IdAndKindAndPeriodStart(projectId, kind, periodStart)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reflection> findPage(
            Long projectId,
            ReflectionKind kind,
            LocalDate from,
            LocalDate to,
            ReflectionStatus status,
            LocalDate cursorPeriodStart,
            Long cursorId,
            int limit
    ) {
        return reflectionJpaRepository.findPage(
                projectId, kind, from, to, status, cursorPeriodStart, cursorId,
                PageRequest.of(0, limit)
        ).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reflection> findSavedDaily(
            Long projectId, LocalDate from, LocalDate to
    ) {
        return reflectionJpaRepository
                .findAllByProject_IdAndKindAndStatusAndPeriodStartBetweenOrderByPeriodStartAsc(
                        projectId, ReflectionKind.DAILY, ReflectionStatus.SAVED, from, to
                ).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public Optional<Reflection> saveIfVersionMatches(
            Long projectId,
            Long reflectionId,
            String title,
            ReflectionDocument content,
            int expectedVersion,
            OffsetDateTime savedAt
    ) {
        Optional<ReflectionJpaEntity> locked =
                reflectionJpaRepository.findForUpdate(projectId, reflectionId);
        if (locked.isEmpty() || locked.orElseThrow().getVersion() != expectedVersion) {
            return Optional.empty();
        }
        ReflectionJpaEntity entity = locked.orElseThrow();
        entity.saveDocument(title, toJson(content), savedAt);
        reflectionJpaRepository.flush();
        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public Optional<Reflection> queueRegeneration(
            Long projectId,
            Long reflectionId
    ) {
        Optional<ReflectionJpaEntity> locked =
                reflectionJpaRepository.findForUpdate(projectId, reflectionId);
        if (locked.isEmpty()) {
            return Optional.empty();
        }
        ReflectionJpaEntity entity = locked.orElseThrow();
        if (entity.getStatus() != ReflectionStatus.QUEUED
                && entity.getStatus() != ReflectionStatus.GENERATING) {
            entity.queue();
            reflectionJpaRepository.flush();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public Optional<Reflection> claimNext(
            OffsetDateTime now, OffsetDateTime stuckBefore
    ) {
        for (ReflectionJpaEntity stuck : reflectionJpaRepository.findStuck(
                ReflectionStatus.GENERATING, stuckBefore
        )) {
            if (stuck.getGenerationAttempts() >= MAX_ATTEMPTS) {
                stuck.failGeneration(
                        "REFLECTION_50001", "중단된 회고 생성 작업이 최대 재시도 횟수를 초과했습니다."
                );
            } else {
                stuck.requeueStuck();
            }
        }

        List<ReflectionJpaEntity> claimable = reflectionJpaRepository.findClaimable(
                ReflectionStatus.QUEUED, PageRequest.of(0, 1)
        );
        if (claimable.isEmpty()) {
            return Optional.empty();
        }
        ReflectionJpaEntity entity = claimable.get(0);
        entity.claim(now);
        reflectionJpaRepository.flush();
        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public void completeGeneration(
            Long reflectionId,
            String title,
            ReflectionDocument content,
            SourceQuality sourceQuality,
            ReflectionSourceSnapshot sourceSnapshot,
            String sourceHash,
            OffsetDateTime generatedAt
    ) {
        ReflectionJpaEntity entity = reflectionJpaRepository.findById(reflectionId)
                .orElseThrow();
        entity.completeGeneration(
                title, toJson(content), sourceQuality, toJson(sourceSnapshot),
                sourceHash, generatedAt
        );
    }

    @Override
    @Transactional
    public void failGeneration(Long reflectionId, String errorCode, String errorMessage) {
        ReflectionJpaEntity entity = reflectionJpaRepository.findById(reflectionId)
                .orElseThrow();
        entity.failGeneration(errorCode, errorMessage);
    }

    private ReflectionJpaEntity toJpaEntity(Reflection reflection) {
        return ReflectionJpaEntity.builder()
                .id(reflection.id())
                .project(ProjectJpaEntity.builder().id(reflection.projectId()).build())
                .kind(reflection.kind())
                .periodStart(reflection.periodStart())
                .periodEnd(reflection.periodEnd())
                .title(reflection.title())
                .content(toJson(reflection.content()))
                .status(reflection.status())
                .sourceQuality(reflection.sourceQuality())
                // Domain snapshot을 JSONB로 변환하고 hash와 함께 동일 reflections 행에 저장
                .sourceSnapshot(toJson(reflection.sourceSnapshot()))
                .sourceHash(reflection.sourceHash())
                .generationAttempts(reflection.generationAttempts())
                .generationStartedAt(reflection.generationStartedAt())
                .generatedAt(reflection.generatedAt())
                .savedAt(reflection.savedAt())
                .errorCode(reflection.error() == null ? null : reflection.error().code())
                .errorMessage(reflection.error() == null ? null : reflection.error().message())
                .version(reflection.version())
                .createdAt(reflection.createdAt())
                .updatedAt(reflection.updatedAt())
                .build();
    }

    private Reflection toDomain(ReflectionJpaEntity entity) {
        return Reflection.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .kind(entity.getKind())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .title(entity.getTitle())
                .content(readDocument(entity.getContent()))
                .status(entity.getStatus())
                .sourceQuality(entity.getSourceQuality())
                .sourceSnapshot(read(entity.getSourceSnapshot(), ReflectionSourceSnapshot.class))
                .sourceHash(entity.getSourceHash())
                .generationAttempts(entity.getGenerationAttempts())
                .generationStartedAt(entity.getGenerationStartedAt())
                .generatedAt(entity.getGeneratedAt())
                .savedAt(entity.getSavedAt())
                .error(entity.getErrorCode() == null ? null : new Reflection.ReflectionError(
                        entity.getErrorCode(), entity.getErrorMessage(), true
                ))
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ReflectionDocument readDocument(JsonNode node) {
        if (node == null || !node.has("schemaVersion")) {
            List<ReflectionDocument.Section> sections = new ArrayList<>();
            if (node != null && node.path("sections").isArray()) {
                node.path("sections").forEach(section ->
                        sections.add(read(section, ReflectionDocument.Section.class)));
            }
            return new ReflectionDocument(ReflectionDocument.CURRENT_SCHEMA_VERSION, sections);
        }
        return read(node, ReflectionDocument.class);
    }

    private JsonNode toJson(Object value) {
        return objectMapper.valueToTree(value);
    }

    private <T> T read(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (Exception exception) {
            throw new IllegalStateException("invalid reflection json", exception);
        }
    }
}
