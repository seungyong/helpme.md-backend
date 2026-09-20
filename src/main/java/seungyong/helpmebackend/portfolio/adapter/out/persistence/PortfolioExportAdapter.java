package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.notion.adapter.out.persistence.entity.NotionConnectionJpaEntity;
import seungyong.helpmebackend.portfolio.adapter.out.persistence.entity.PortfolioExportJpaEntity;
import seungyong.helpmebackend.portfolio.adapter.out.persistence.entity.PortfolioJpaEntity;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.global.application.pagination.CursorPagination;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PortfolioExportAdapter implements PortfolioExportPortOut {
    private static final short MAX_ATTEMPTS = 3;

    private final PortfolioExportJpaRepository repository;
    private final PortfolioJpaRepository portfolioRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PortfolioExport save(PortfolioExport export) {
        PortfolioExportJpaEntity entity = PortfolioExportJpaEntity.builder()
                .portfolio(portfolioRepository.getReferenceById(export.portfolioId()))
                .notionConnection(export.notionConnectionId() == null ? null
                        : NotionConnectionJpaEntity.builder().id(export.notionConnectionId()).build())
                .format(export.format()).status(export.status()).idempotencyKey(export.idempotencyKey())
                .portfolioVersion(export.portfolioVersion())
                .documentSnapshot(json(export.document())).options(json(export.options()))
                .notionParentPageId(export.notionParentPageId()).attempts(export.attempts())
                .build();
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioExport> getByIdempotencyKey(UUID idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioExport> getByPortfolioIdAndId(Long portfolioId, Long exportId) {
        return repository.findByPortfolioIdAndId(portfolioId, exportId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioExport> findPage(Long portfolioId, PortfolioExportFormat format,
                                          PortfolioExportStatus status,
                                          CursorPagination<OffsetDateTime> pagination) {
        return repository.findPage(
                portfolioId, format, status,
                pagination.cursorValue(), pagination.cursorId(),
                PageRequest.of(0, pagination.queryLimit())
        ).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public Optional<PortfolioExport> claimNext(OffsetDateTime now, OffsetDateTime stuckBefore) {
        for (PortfolioExportJpaEntity stuck : repository.findStuck(
                PortfolioExportStatus.PROCESSING, ProjectStatus.ACTIVE, stuckBefore)) {
            if (stuck.getAttempts() >= MAX_ATTEMPTS) {
                stuck.fail("EXPORT_50001", "내보내기 작업이 제한 시간 안에 완료되지 않았습니다.", now);
            } else {
                stuck.requeueStuck();
            }
        }
        List<PortfolioExportJpaEntity> claimable = repository.findClaimable(
                PortfolioExportStatus.QUEUED, PortfolioExportStatus.PROCESSING,
                ProjectStatus.ACTIVE, PageRequest.of(0, 1));
        if (claimable.isEmpty()) return Optional.empty();
        PortfolioExportJpaEntity entity = claimable.get(0);
        entity.claim(now);
        repository.flush();
        return Optional.of(toDomain(entity));
    }

    @Override
    @Transactional
    public void completePdf(Long exportId, String storagePath, String fileName, long fileSizeBytes,
                            int pageCount, OffsetDateTime expiresAt, OffsetDateTime completedAt) {
        getEntity(exportId).completePdf(storagePath, fileName, fileSizeBytes, pageCount, expiresAt, completedAt);
    }

    @Override
    @Transactional
    public void completeNotion(Long exportId, String pageId, String pageUrl, OffsetDateTime completedAt) {
        getEntity(exportId).completeNotion(pageId, pageUrl, completedAt);
    }

    @Override
    @Transactional
    public void requireConflictAction(Long exportId, String pageId, String pageTitle, String pageUrl) {
        getEntity(exportId).requireConflictAction(pageId, pageTitle, pageUrl);
    }

    @Override
    @Transactional
    public Optional<PortfolioExport> retry(Long exportId) {
        return repository.findById(exportId).filter(e -> e.getStatus() == PortfolioExportStatus.FAILED)
                .map(e -> { e.retry(); return toDomain(e); });
    }

    @Override
    @Transactional
    public Optional<PortfolioExport> resolveConflict(Long exportId, PortfolioConflictAction action) {
        return repository.findById(exportId).filter(e -> e.getStatus() == PortfolioExportStatus.NEEDS_ACTION)
                .map(e -> { e.resolveConflict(action); return toDomain(e); });
    }

    @Override
    @Transactional
    public void fail(Long exportId, String errorCode, String errorMessage, OffsetDateTime completedAt) {
        repository.findById(exportId).ifPresent(entity ->
                entity.fail(errorCode, errorMessage, completedAt));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioExport> findExpiredPdf(OffsetDateTime now, int limit) {
        return repository.findExpired(PortfolioExportFormat.PDF, PortfolioExportStatus.SUCCEEDED,
                now, PageRequest.of(0, limit)).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPdfStoragePathsByProjectId(Long projectId) {
        // 업로드 후 DB 저장 실패도 정리하도록 export ID로 재구성한 경로와 저장된 경로 모두 포함
        java.util.Set<String> paths = new java.util.LinkedHashSet<>();
        for (var asset : repository.findPdfAssetsByProjectId(projectId, PortfolioExportFormat.PDF)) {
            paths.add(seungyong.helpmebackend.portfolio.domain.entity.PortfolioPdfStoragePath.of(
                    asset.getPortfolioId(), asset.getExportId()));
            if (org.springframework.util.StringUtils.hasText(asset.getStoragePath())) {
                paths.add(asset.getStoragePath());
            }
        }
        return List.copyOf(paths);
    }

    @Override
    @Transactional
    public void markExpired(Long exportId) {
        getEntity(exportId).expire();
    }

    private PortfolioExportJpaEntity getEntity(Long exportId) {
        return repository.findById(exportId).orElseThrow();
    }

    private JsonNode json(Object value) {
        return objectMapper.valueToTree(value);
    }

    private PortfolioExport toDomain(PortfolioExportJpaEntity entity) {
        JsonNode metadata = entity.getResultMetadata();
        return PortfolioExport.builder()
                .id(entity.getId()).portfolioId(entity.getPortfolio().getId())
                .projectId(entity.getPortfolio().getProject().getId())
                .notionConnectionId(entity.getNotionConnection() == null ? null : entity.getNotionConnection().getId())
                .format(entity.getFormat()).status(entity.getStatus()).idempotencyKey(entity.getIdempotencyKey())
                .portfolioVersion(entity.getPortfolioVersion())
                .document(value(entity.getDocumentSnapshot(), PortfolioExportDocument.class))
                .options(value(entity.getOptions(), PortfolioExportOptions.class))
                .storagePath(entity.getStoragePath()).fileName(entity.getFileName())
                .fileSizeBytes(entity.getFileSizeBytes()).pageCount(entity.getPageCount()).expiresAt(entity.getExpiresAt())
                .notionParentPageId(entity.getNotionParentPageId()).notionPageId(entity.getNotionPageId())
                .notionPageUrl(entity.getNotionPageUrl()).conflictAction(entity.getConflictAction())
                .conflictPageId(text(metadata, "conflictPageId")).conflictPageTitle(text(metadata, "conflictPageTitle"))
                .conflictPageUrl(text(metadata, "conflictPageUrl"))
                .attempts(entity.getAttempts()).errorCode(entity.getErrorCode()).errorMessage(entity.getErrorMessage())
                .startedAt(entity.getStartedAt()).completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }

    private <T> T value(JsonNode node, Class<T> type) {
        return objectMapper.convertValue(node, type);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
