package seungyong.helpmebackend.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Table(name = "users")
@Entity(name = "User")
public class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(name = "github_token", nullable = false, unique = true)
    private String githubToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPZ DEFAULT now()")
    private LocalDateTime createdAt;
}
