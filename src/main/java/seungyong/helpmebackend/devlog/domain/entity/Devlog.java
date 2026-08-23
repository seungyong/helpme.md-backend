package seungyong.helpmebackend.devlog.domain.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Devlog(
        Long id,
        Long projectId,
        LocalDate logDate,
        String contentMarkdown,
        Integer version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Devlog {
        if (projectId == null || logDate == null || contentMarkdown == null) {
            throw new IllegalArgumentException("project, log date and content are required");
        }
        if (version != null && version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        if (id == null && version != null) {
            throw new IllegalArgumentException("empty devlog must not have a version");
        }
        if (id != null && version == null) {
            throw new IllegalArgumentException("persisted devlog requires a version");
        }
    }

    public static Devlog empty(Long projectId, LocalDate logDate) {
        return new Devlog(null, projectId, logDate, "", null, null, null);
    }

    public boolean exists() {
        return id != null;
    }
}
