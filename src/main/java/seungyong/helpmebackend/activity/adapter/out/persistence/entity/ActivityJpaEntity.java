package seungyong.helpmebackend.activity.adapter.out.persistence.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.adapter.out.persistence.converter.DatabaseEnumConverters;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.webhook.adapter.out.persistence.entity.WebhookDeliveryJpaEntity;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "activities",
        uniqueConstraints = @UniqueConstraint(
                name = "activities_project_external_uk",
                columnNames = {"project_id", "external_key"}
        )
)
@Entity(name = "Activity")
public class ActivityJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectJpaEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_delivery_id")
    private WebhookDeliveryJpaEntity webhookDelivery;

    @Column(name = "external_key", nullable = false, columnDefinition = "TEXT")
    private String externalKey;

    @Convert(converter = DatabaseEnumConverters.ActivityTypeConverter.class)
    @Column(name = "activity_type", nullable = false, columnDefinition = "TEXT")
    private ActivityType activityType;

    @Column(name = "branch_name", columnDefinition = "TEXT")
    private String branchName;

    @Column(name = "commit_sha", columnDefinition = "TEXT")
    private String commitSha;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "actor_login", columnDefinition = "TEXT")
    private String actorLogin;

    @Column(name = "public_url", columnDefinition = "TEXT")
    private String publicUrl;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", nullable = false)
    private JsonNode details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    public ActivityJpaEntity(
            Long id,
            ProjectJpaEntity project,
            WebhookDeliveryJpaEntity webhookDelivery,
            String externalKey,
            ActivityType activityType,
            String branchName,
            String commitSha,
            String title,
            String summary,
            String actorLogin,
            String publicUrl,
            Integer additions,
            Integer deletions,
            Integer filesChanged,
            OffsetDateTime occurredAt,
            JsonNode details,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.project = project;
        this.webhookDelivery = webhookDelivery;
        this.externalKey = externalKey;
        this.activityType = activityType;
        this.branchName = branchName;
        this.commitSha = commitSha;
        this.title = title;
        this.summary = summary;
        this.actorLogin = actorLogin;
        this.publicUrl = publicUrl;
        this.additions = additions;
        this.deletions = deletions;
        this.filesChanged = filesChanged;
        this.occurredAt = occurredAt;
        this.details = details == null ? JsonNodeFactory.instance.objectNode() : details;
        this.createdAt = createdAt;
    }
}
