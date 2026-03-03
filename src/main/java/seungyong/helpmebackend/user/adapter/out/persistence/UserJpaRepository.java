package seungyong.helpmebackend.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;

import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByGithubId(Long githubId);
}
