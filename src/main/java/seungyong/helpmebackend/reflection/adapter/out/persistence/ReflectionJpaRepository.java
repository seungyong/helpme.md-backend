package seungyong.helpmebackend.reflection.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.reflection.adapter.out.persistence.entity.ReflectionJpaEntity;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

interface ReflectionJpaRepository extends JpaRepository<ReflectionJpaEntity, Long> {
    Optional<ReflectionJpaEntity> findByProject_IdAndId(Long projectId, Long id);

    Optional<ReflectionJpaEntity> findByProject_IdAndKindAndPeriodStart(
            Long projectId, ReflectionKind kind, LocalDate periodStart
    );

    @Query("""
            select r from Reflection r
            where r.project.id = :projectId
              and r.kind = :kind
              and (:from is null or r.periodStart >= :from)
              and (:to is null or r.periodEnd <= :to)
              and (:status is null or r.status = :status)
              and (:cursorPeriodStart is null or r.periodStart < :cursorPeriodStart
                   or (r.periodStart = :cursorPeriodStart and r.id < :cursorId))
            order by r.periodStart desc, r.id desc
            """)
    List<ReflectionJpaEntity> findPage(
            @Param("projectId") Long projectId,
            @Param("kind") ReflectionKind kind,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") ReflectionStatus status,
            @Param("cursorPeriodStart") LocalDate cursorPeriodStart,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    List<ReflectionJpaEntity>
    findAllByProject_IdAndKindAndStatusAndPeriodStartBetweenOrderByPeriodStartAsc(
            Long projectId,
            ReflectionKind kind,
            ReflectionStatus status,
            LocalDate from,
            LocalDate to
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reflection r where r.project.id = :projectId and r.id = :id")
    Optional<ReflectionJpaEntity> findForUpdate(
            @Param("projectId") Long projectId,
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reflection r
            where r.status = :queued
            order by r.createdAt asc, r.id asc
            """)
    List<ReflectionJpaEntity> findClaimable(
            @Param("queued") ReflectionStatus queued,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reflection r
            where r.status = :generating
              and r.generationStartedAt < :stuckBefore
            """)
    List<ReflectionJpaEntity> findStuck(
            @Param("generating") ReflectionStatus generating,
            @Param("stuckBefore") OffsetDateTime stuckBefore
    );
}
