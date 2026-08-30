package seungyong.helpmebackend.section.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.section.adapter.out.persistence.entity.SectionJpaEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

interface SectionJpaRepository extends JpaRepository<SectionJpaEntity, Long> {
    @Query(
        "SELECT s " +
        "FROM Section s " +
        "JOIN FETCH s.project p " +
        "WHERE p.user.id = :userId AND p.repoFullName = :repoFullName " +
        "ORDER BY s.orderIdx ASC"
    )
    List<SectionJpaEntity> findAllByUserIdAndRepoFullName(Long userId, String repoFullName);

    @Query(
        "SELECT s " +
        "FROM Section s " +
        "JOIN FETCH s.project p " +
        "WHERE p.user.id = :userId AND p.repoFullName = :repoFullName " +
        "ORDER BY s.orderIdx DESC LIMIT 1"
    )
    Optional<SectionJpaEntity> findLastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName);

    Optional<SectionJpaEntity> findByIdAndProject_User_Id(Long sectionId, Long userId);

    Optional<SectionJpaEntity> findByIdAndProject_User_IdAndProject_RepoFullName(
            Long sectionId,
            Long userId,
            String repoFullName
    );

    @Modifying
    @Query(
        "DELETE FROM Section s " +
        "WHERE s.project.user.id = :userId AND s.project.repoFullName = :repoFullName "
    )
    void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName);

    @Modifying
    @Query(
            "UPDATE Section s " +
            "SET s.orderIdx = s.orderIdx - 1 " +
            "WHERE s.project.user.id = :userId " +
            "AND s.project.repoFullName = :repoFullName " +
            "AND s.orderIdx > :targetIdx"
    )
    void decreaseOrderIdxAfter(Long userId, String repoFullName, Integer targetIdx);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Section s
               set s.orderIdx = s.orderIdx + 1,
                   s.version = s.version + 1,
                   s.updatedAt = :updatedAt
             where s.project.user.id = :userId
               and s.project.repoFullName = :repoFullName
               and s.orderIdx >= :targetIdx
            """)
    void increaseOrderIdxFrom(
            @Param("userId") Long userId,
            @Param("repoFullName") String repoFullName,
            @Param("targetIdx") Integer targetIdx,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Section s
               set s.orderIdx = s.orderIdx - 1,
                   s.version = s.version + 1,
                   s.updatedAt = :updatedAt
             where s.project.user.id = :userId
               and s.project.repoFullName = :repoFullName
               and s.id <> :sectionId
               and s.orderIdx > :currentIdx
               and s.orderIdx <= :targetIdx
            """)
    void shiftOrderIdxDownForMove(
            @Param("userId") Long userId,
            @Param("repoFullName") String repoFullName,
            @Param("sectionId") Long sectionId,
            @Param("currentIdx") Integer currentIdx,
            @Param("targetIdx") Integer targetIdx,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Section s
               set s.orderIdx = s.orderIdx + 1,
                   s.version = s.version + 1,
                   s.updatedAt = :updatedAt
             where s.project.user.id = :userId
               and s.project.repoFullName = :repoFullName
               and s.id <> :sectionId
               and s.orderIdx >= :targetIdx
               and s.orderIdx < :currentIdx
            """)
    void shiftOrderIdxUpForMove(
            @Param("userId") Long userId,
            @Param("repoFullName") String repoFullName,
            @Param("sectionId") Long sectionId,
            @Param("currentIdx") Integer currentIdx,
            @Param("targetIdx") Integer targetIdx,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Section s
               set s.title = :title,
                   s.content = :content,
                   s.orderIdx = :orderIdx,
                   s.version = s.version + 1,
                   s.updatedAt = :updatedAt
             where s.id = :sectionId
               and s.project.user.id = :userId
               and s.project.repoFullName = :repoFullName
               and s.version = :expectedVersion
            """)
    int updateIfVersionMatches(
            @Param("sectionId") Long sectionId,
            @Param("userId") Long userId,
            @Param("repoFullName") String repoFullName,
            @Param("title") String title,
            @Param("content") String content,
            @Param("orderIdx") Integer orderIdx,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from Section s
             where s.id = :sectionId
               and s.project.user.id = :userId
               and s.project.repoFullName = :repoFullName
               and s.version = :expectedVersion
            """)
    int deleteIfVersionMatches(
            @Param("sectionId") Long sectionId,
            @Param("userId") Long userId,
            @Param("repoFullName") String repoFullName,
            @Param("expectedVersion") Integer expectedVersion
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Section s
               set s.orderIdx = s.orderIdx - 1,
                   s.version = s.version + 1,
                   s.updatedAt = :updatedAt
             where s.project.user.id = :userId
               and s.project.repoFullName = :repoFullName
               and s.orderIdx > :targetIdx
            """)
    void decreaseOrderIdxAfterVersioned(
            @Param("userId") Long userId,
            @Param("repoFullName") String repoFullName,
            @Param("targetIdx") Integer targetIdx,
            @Param("updatedAt") OffsetDateTime updatedAt
    );
}
