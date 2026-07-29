package seungyong.helpmebackend.portfolio.adapter.out.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import seungyong.helpmebackend.global.adapter.out.persistence.converter.DatabaseEnumConverters;
import seungyong.helpmebackend.notion.adapter.out.persistence.entity.NotionConnectionJpaEntity;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_exports")
@Entity(name = "PortfolioExport")
public class PortfolioExportJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioJpaEntity portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notion_connection_id")
    private NotionConnectionJpaEntity notionConnection;

    @Convert(converter = DatabaseEnumConverters.PortfolioExportFormatConverter.class)
    @Column(name = "format", nullable = false, columnDefinition = "TEXT")
    private PortfolioExportFormat format;

    @Convert(converter = DatabaseEnumConverters.PortfolioExportStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private PortfolioExportStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "portfolio_version", nullable = false)
    private Integer portfolioVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_snapshot", nullable = false)
    private JsonNode documentSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", nullable = false)
    private JsonNode options;

    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "file_name", columnDefinition = "TEXT")
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "notion_parent_page_id", columnDefinition = "TEXT")
    private String notionParentPageId;

    @Column(name = "notion_page_id", columnDefinition = "TEXT")
    private String notionPageId;

    @Column(name = "notion_page_url", columnDefinition = "TEXT")
    private String notionPageUrl;

    @Convert(converter = DatabaseEnumConverters.PortfolioConflictActionConverter.class)
    @Column(name = "conflict_action", columnDefinition = "TEXT")
    private PortfolioConflictAction conflictAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_metadata", nullable = false)
    private JsonNode resultMetadata;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "error_code", columnDefinition = "TEXT")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public PortfolioExportJpaEntity(
            Long id,
            PortfolioJpaEntity portfolio,
            NotionConnectionJpaEntity notionConnection,
            PortfolioExportFormat format,
            PortfolioExportStatus status,
            UUID idempotencyKey,
            Integer portfolioVersion,
            JsonNode documentSnapshot,
            JsonNode options,
            String storagePath,
            String fileName,
            Long fileSizeBytes,
            Integer pageCount,
            OffsetDateTime expiresAt,
            String notionParentPageId,
            String notionPageId,
            String notionPageUrl,
            PortfolioConflictAction conflictAction,
            JsonNode resultMetadata,
            Short attempts,
            OffsetDateTime nextRetryAt,
            String errorCode,
            String errorMessage,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.portfolio = portfolio;
        this.notionConnection = notionConnection;
        this.format = format;
        this.status = status == null ? PortfolioExportStatus.QUEUED : status;
        this.idempotencyKey = idempotencyKey;
        this.portfolioVersion = portfolioVersion == null ? 0 : portfolioVersion;
        this.documentSnapshot = documentSnapshot;
        this.options = options == null ? JsonNodeFactory.instance.objectNode() : options;
        this.storagePath = storagePath;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.pageCount = pageCount;
        this.expiresAt = expiresAt;
        this.notionParentPageId = notionParentPageId;
        this.notionPageId = notionPageId;
        this.notionPageUrl = notionPageUrl;
        this.conflictAction = conflictAction;
        this.resultMetadata = resultMetadata == null ? JsonNodeFactory.instance.objectNode() : resultMetadata;
        this.attempts = attempts == null ? (short) 0 : attempts;
        this.nextRetryAt = nextRetryAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
