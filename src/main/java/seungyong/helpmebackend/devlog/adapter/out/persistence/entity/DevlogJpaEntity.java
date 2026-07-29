package seungyong.helpmebackend.devlog.adapter.out.persistence.entity;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "devlogs",
        uniqueConstraints = @UniqueConstraint(
                name = "devlogs_project_date_uk",
                columnNames = {"project_id", "log_date"}
        )
)
@Entity(name = "Devlog")
public class DevlogJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectJpaEntity project;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMarkdown;

    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public DevlogJpaEntity(
            Long id,
            ProjectJpaEntity project,
            LocalDate logDate,
            String contentMarkdown,
            Integer version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.project = project;
        this.logDate = logDate;
        this.contentMarkdown = contentMarkdown == null ? "" : contentMarkdown;
        this.version = version == null ? 0 : version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
