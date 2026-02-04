package seungyong.helpmebackend.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import seungyong.helpmebackend.adapter.out.persistence.entity.SectionJpaEntity;
import seungyong.helpmebackend.domain.entity.section.Section;

import java.util.List;
import java.util.Optional;

public interface SectionJpaRepository extends JpaRepository<SectionJpaEntity, Long> {
    @Query(
        "SELECT s " +
        "FROM Section s " +
        "JOIN FETCH s.project p " +
        "WHERE p.user.id = :userId AND p.repoFullName = :repoFullName "
    )
    List<SectionJpaEntity> findAllByUserIdAndRepoFullName(Long userId, String repoFullName);

    @Query(
        "SELECT s " +
        "FROM Section s " +
        "JOIN s.project p " +
        "WHERE p.user.id = :userId AND p.repoFullName = :repoFullName " +
        "ORDER BY s.orderIdx DESC LIMIT 1"
    )
    Optional<SectionJpaEntity> findLastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName);

    @Modifying
    @Query(
        "DELETE FROM Section s " +
        "WHERE s.project.user.id = :userId AND s.project.repoFullName = :repoFullName "
    )
    void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName);
}
