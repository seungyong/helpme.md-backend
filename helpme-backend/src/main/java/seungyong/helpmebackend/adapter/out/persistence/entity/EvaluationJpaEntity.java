package seungyong.helpmebackend.adapter.out.persistence.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;
import seungyong.helpmebackend.domain.vo.EvaluationStatus;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@Table(name = "evaluations")
@Entity(name = "Evaluation")
public class EvaluationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "uploader_id", nullable = false)
    private UserJpaEntity uploader;

    @Column(name = "repo_fullname", nullable = false, unique = true, columnDefinition = "TEXT")
    private String repoFullName;

    @Column(name = "rating")
    private Float rating;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "evaluations_status")
    private EvaluationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPZ DEFAULT now()")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPZ DEFAULT now()")
    private LocalDateTime updatedAt;
}
