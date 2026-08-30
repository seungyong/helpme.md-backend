package seungyong.helpmebackend.reflection.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionPage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ResponseReflections(
        List<Item> items,
        CurrentPeriod currentPeriod,
        Page page
) {
    public static ResponseReflections from(ReflectionPage result) {
        return new ResponseReflections(
                result.items().stream().map(Item::from).toList(),
                CurrentPeriod.from(result.currentPeriod()),
                new Page(result.nextCursor(), result.hasNext())
        );
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Item(
            Long id,
            String kind,
            LocalDate periodStart,
            LocalDate periodEnd,
            String title,
            String status,
            String sourceQuality,
            String summary,
            int activityCount,
            boolean devlogExists,
            int version,
            OffsetDateTime generatedAt,
            OffsetDateTime savedAt,
            ResponseReflectionDetail.Error error
    ) {
        private static Item from(Reflection reflection) {
            return new Item(
                    reflection.id(),
                    reflection.kind().getDatabaseValue(),
                    reflection.periodStart(),
                    reflection.periodEnd(),
                    reflection.title(),
                    reflection.status().getDatabaseValue(),
                    reflection.sourceQuality().getDatabaseValue(),
                    reflection.summary(),
                    reflection.sourceSnapshot().activityCount(),
                    reflection.sourceSnapshot().devlogCount() > 0,
                    reflection.version(),
                    reflection.generatedAt(),
                    reflection.savedAt(),
                    ResponseReflectionDetail.Error.from(reflection.error())
            );
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CurrentPeriod(
            boolean exists,
            Long reflectionId,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status,
            String sourceQuality,
            Integer version,
            ResponseReflectionDetail.Error error,
            boolean canGenerate,
            String reason,
            OffsetDateTime scheduledAt
    ) {
        private static CurrentPeriod from(ReflectionPage.CurrentPeriod current) {
            return new CurrentPeriod(
                    current.exists(),
                    current.reflectionId(),
                    current.periodStart(),
                    current.periodEnd(),
                    current.status() == null
                            ? null : current.status().getDatabaseValue(),
                    current.sourceQuality() == null
                            ? null : current.sourceQuality().getDatabaseValue(),
                    current.version(),
                    ResponseReflectionDetail.Error.from(current.error()),
                    current.canGenerate(),
                    current.reason(),
                    current.scheduledAt()
            );
        }
    }

    public record Page(String nextCursor, boolean hasNext) {
    }
}
