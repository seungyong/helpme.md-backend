package seungyong.helpmebackend.devlog.application.port.out;

import seungyong.helpmebackend.devlog.domain.entity.Devlog;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

public interface DevlogPortOut {
    Optional<Devlog> getByProjectIdAndLogDate(Long projectId, LocalDate logDate);

    List<Devlog> getByProjectIdAndLogDateBetween(
            Long projectId, LocalDate from, LocalDate to
    );

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
