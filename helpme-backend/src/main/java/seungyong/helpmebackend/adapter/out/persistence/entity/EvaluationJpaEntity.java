package seungyong.helpmebackend.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import seungyong.helpmebackend.domain.vo.EvaluationStatus;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "repo_id", nullable = false)
    private RepositoryJpaEntity repository;

    @Column(name = "rating", nullable = false)
    private Float rating;

    @Column(name = "content", nullable = false, columnDefinition = "JSONB")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EvaluationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPZ DEFAULT now()")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPZ DEFAULT now()")
    private LocalDateTime updatedAt;
}
