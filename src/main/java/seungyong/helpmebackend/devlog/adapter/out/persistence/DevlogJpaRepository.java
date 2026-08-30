package seungyong.helpmebackend.devlog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.devlog.adapter.out.persistence.entity.DevlogJpaEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

interface DevlogJpaRepository extends JpaRepository<DevlogJpaEntity, Long> {
    Optional<DevlogJpaEntity> findByProject_IdAndLogDate(Long projectId, LocalDate logDate);

    List<DevlogJpaEntity> findAllByProject_IdAndLogDateBetweenOrderByLogDateAsc(
            Long projectId, LocalDate from, LocalDate to
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Devlog d
               set d.contentMarkdown = :contentMarkdown,
                   d.version = d.version + 1,
                   d.updatedAt = :updatedAt
             where d.project.id = :projectId
               and d.logDate = :logDate
               and d.version = :expectedVersion
            """)
    int updateIfVersionMatches(
            @Param("projectId") Long projectId,
            @Param("logDate") LocalDate logDate,
            @Param("contentMarkdown") String contentMarkdown,
            @Param("expectedVersion") int expectedVersion,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from Devlog d
             where d.project.id = :projectId
               and d.logDate = :logDate
               and d.version = :expectedVersion
            """)
    int deleteIfVersionMatches(
            @Param("projectId") Long projectId,
            @Param("logDate") LocalDate logDate,
            @Param("expectedVersion") int expectedVersion
    );
}
