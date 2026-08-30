package seungyong.helpmebackend.reflection.domain.entity;

import lombok.Builder;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Builder
public record Reflection(
        Long id,
        Long projectId,
        ReflectionKind kind,
        LocalDate periodStart,
        LocalDate periodEnd,
        String title,
        ReflectionDocument content,
        ReflectionStatus status,
        SourceQuality sourceQuality,
        ReflectionSourceSnapshot sourceSnapshot,
        String sourceHash,
        short generationAttempts,
        OffsetDateTime generationStartedAt,
        OffsetDateTime generatedAt,
        OffsetDateTime savedAt,
        ReflectionError error,
        int version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Reflection {
        if (projectId == null || kind == null || periodStart == null || periodEnd == null
                || content == null || status == null || sourceQuality == null
                || sourceSnapshot == null || generationAttempts < 0 || version < 0) {
            throw new IllegalArgumentException("invalid reflection");
        }
    }

    public String summary() {
        return content.summary();
    }

    public boolean isGenerating() {
        return status == ReflectionStatus.QUEUED || status == ReflectionStatus.GENERATING;
    }

    public record ReflectionError(String code, String message, boolean retryable) {
    }
}
