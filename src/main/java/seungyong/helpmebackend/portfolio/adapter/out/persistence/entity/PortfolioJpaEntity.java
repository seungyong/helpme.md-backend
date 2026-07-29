package seungyong.helpmebackend.portfolio.adapter.out.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolios")
@Entity(name = "Portfolio")
public class PortfolioJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectJpaEntity project;

    @Column(name = "request_key", nullable = false, unique = true)
    private UUID requestKey;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Convert(converter = DatabaseEnumConverters.PortfolioToneConverter.class)
    @Column(name = "tone", nullable = false, columnDefinition = "TEXT")
    private PortfolioTone tone;

    @Convert(converter = DatabaseEnumConverters.PortfolioStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private PortfolioStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false)
    private JsonNode content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_snapshot", nullable = false)
    private JsonNode sourceSnapshot;

    @Column(name = "source_hash", columnDefinition = "TEXT")
    private String sourceHash;

    @Column(name = "generation_attempts", nullable = false)
    private short generationAttempts;

    @Column(name = "generation_started_at")
    private OffsetDateTime generationStartedAt;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(name = "saved_at")
    private OffsetDateTime savedAt;

    @Column(name = "error_code", columnDefinition = "TEXT")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public PortfolioJpaEntity(
            Long id,
            ProjectJpaEntity project,
            UUID requestKey,
            String title,
            LocalDate periodStart,
            LocalDate periodEnd,
            PortfolioTone tone,
            PortfolioStatus status,
            JsonNode content,
            JsonNode sourceSnapshot,
            String sourceHash,
            Short generationAttempts,
            OffsetDateTime generationStartedAt,
            OffsetDateTime generatedAt,
            OffsetDateTime savedAt,
            String errorCode,
            String errorMessage,
            Integer version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.project = project;
        this.requestKey = requestKey;
        this.title = title;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.tone = tone == null ? PortfolioTone.CONCISE : tone;
        this.status = status == null ? PortfolioStatus.QUEUED : status;
        this.content = content == null ? emptyDocument() : content;
        this.sourceSnapshot = sourceSnapshot == null ? JsonNodeFactory.instance.objectNode() : sourceSnapshot;
        this.sourceHash = sourceHash;
        this.generationAttempts = generationAttempts == null ? (short) 0 : generationAttempts;
        this.generationStartedAt = generationStartedAt;
        this.generatedAt = generatedAt;
        this.savedAt = savedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.version = version == null ? 0 : version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static ObjectNode emptyDocument() {
        ObjectNode document = JsonNodeFactory.instance.objectNode();
        document.set("sections", JsonNodeFactory.instance.arrayNode());
        return document;
    }
}
