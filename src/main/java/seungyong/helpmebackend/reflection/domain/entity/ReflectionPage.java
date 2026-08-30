package seungyong.helpmebackend.reflection.domain.entity;

import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ReflectionPage(
        List<Reflection> items,
        CurrentPeriod currentPeriod,
        String nextCursor,
        boolean hasNext
) {
    public ReflectionPage {
        items = List.copyOf(items);
    }

    public record CurrentPeriod(
            boolean exists,
            Long reflectionId,
            ReflectionKind kind,
            LocalDate periodStart,
            LocalDate periodEnd,
            ReflectionStatus status,
            SourceQuality sourceQuality,
            Integer version,
            Reflection.ReflectionError error,
            boolean canGenerate,
            String reason,
            OffsetDateTime scheduledAt
    ) {
    }
}
