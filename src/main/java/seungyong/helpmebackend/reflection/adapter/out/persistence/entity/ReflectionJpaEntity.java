package seungyong.helpmebackend.reflection.adapter.out.persistence.entity;

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
import jakarta.persistence.UniqueConstraint;
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
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reflections",
        uniqueConstraints = @UniqueConstraint(
                name = "reflections_project_period_uk",
                columnNames = {"project_id", "kind", "period_start"}
        )
)
@Entity(name = "Reflection")
public class ReflectionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectJpaEntity project;

    @Convert(converter = DatabaseEnumConverters.ReflectionKindConverter.class)
    @Column(name = "kind", nullable = false, columnDefinition = "TEXT")
    private ReflectionKind kind;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false)
    private JsonNode content;

    @Convert(converter = DatabaseEnumConverters.ReflectionStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private ReflectionStatus status;

    @Convert(converter = DatabaseEnumConverters.SourceQualityConverter.class)
    @Column(name = "source_quality", nullable = false, columnDefinition = "TEXT")
    private SourceQuality sourceQuality;

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
    public ReflectionJpaEntity(
            Long id,
            ProjectJpaEntity project,
            ReflectionKind kind,
            LocalDate periodStart,
            LocalDate periodEnd,
            String title,
            JsonNode content,
            ReflectionStatus status,
            SourceQuality sourceQuality,
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
        this.kind = kind;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.title = title;
        this.content = content == null ? emptyDocument() : content;
        this.status = status == null ? ReflectionStatus.QUEUED : status;
        this.sourceQuality = sourceQuality == null ? SourceQuality.COMPLETE : sourceQuality;
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
