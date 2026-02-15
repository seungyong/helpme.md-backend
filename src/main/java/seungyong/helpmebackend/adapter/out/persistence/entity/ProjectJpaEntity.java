package seungyong.helpmebackend.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "projects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_projects_user_repo",
                        columnNames = {"user_id", "repo_fullname"}
                )
        }
)
@Entity(name = "Project")
public class ProjectJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "repo_fullname", nullable = false, columnDefinition = "TEXT")
    private String repoFullName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
