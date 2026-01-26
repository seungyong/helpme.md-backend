package seungyong.helpmebackend.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.adapter.out.persistence.entity.ComponentJpaEntity;

import java.util.List;
import java.util.Optional;

public interface ComponentJpaRepository extends JpaRepository<ComponentJpaEntity, Long> {
    List<ComponentJpaEntity> findByRepoFullNameAndUser_IdOrderByUpdatedAtDesc(String repoFullName, Long userId);
    Optional<ComponentJpaEntity> findByIdAndUser_Id(Long id, Long userId);
}
