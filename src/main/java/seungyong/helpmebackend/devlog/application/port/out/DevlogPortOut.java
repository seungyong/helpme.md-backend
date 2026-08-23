package seungyong.helpmebackend.devlog.application.port.out;

import seungyong.helpmebackend.devlog.domain.entity.Devlog;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface DevlogPortOut {
    Optional<Devlog> getByProjectIdAndLogDate(Long projectId, LocalDate logDate);

    Devlog create(Long projectId, LocalDate logDate, String contentMarkdown);

    Optional<Devlog> updateIfVersionMatches(
            Long projectId,
            LocalDate logDate,
            String contentMarkdown,
            int expectedVersion,
            OffsetDateTime updatedAt
    );

    boolean deleteIfVersionMatches(Long projectId, LocalDate logDate, int expectedVersion);
}
