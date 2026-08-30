package seungyong.helpmebackend.devlog.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.devlog.adapter.out.persistence.entity.DevlogJpaEntity;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DevlogAdapter implements DevlogPortOut {
    private final DevlogJpaRepository devlogJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Devlog> getByProjectIdAndLogDate(Long projectId, LocalDate logDate) {
        return devlogJpaRepository.findByProject_IdAndLogDate(projectId, logDate)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Devlog> getByProjectIdAndLogDateBetween(
            Long projectId, LocalDate from, LocalDate to
    ) {
        return devlogJpaRepository
                .findAllByProject_IdAndLogDateBetweenOrderByLogDateAsc(projectId, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Devlog create(Long projectId, LocalDate logDate, String contentMarkdown) {
        try {
            DevlogJpaEntity entity = DevlogJpaEntity.builder()
                    .project(ProjectJpaEntity.builder().id(projectId).build())
                    .logDate(logDate)
                    .contentMarkdown(contentMarkdown)
                    .version(0)
                    .build();
            return toDomain(devlogJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw versionConflict();
        }
    }

    @Override
    @Transactional
    public Optional<Devlog> updateIfVersionMatches(
            Long projectId,
            LocalDate logDate,
            String contentMarkdown,
            int expectedVersion,
            OffsetDateTime updatedAt
    ) {
        int changed = devlogJpaRepository.updateIfVersionMatches(
                projectId, logDate, contentMarkdown, expectedVersion, updatedAt
        );
        if (changed == 0) {
            return Optional.empty();
        }
        return devlogJpaRepository.findByProject_IdAndLogDate(projectId, logDate)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public boolean deleteIfVersionMatches(
            Long projectId, LocalDate logDate, int expectedVersion
    ) {
        return devlogJpaRepository.deleteIfVersionMatches(
                projectId, logDate, expectedVersion
        ) == 1;
    }

    private Devlog toDomain(DevlogJpaEntity entity) {
        return new Devlog(
                entity.getId(),
                entity.getProject().getId(),
                entity.getLogDate(),
                entity.getContentMarkdown(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CustomException versionConflict() {
        return new CustomException(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
    }
}
