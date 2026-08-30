package seungyong.helpmebackend.reflection.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResponseReflectionDetail(
        Long id,
        String kind,
        LocalDate periodStart,
        LocalDate periodEnd,
        String title,
        ReflectionDocument content,
        String status,
        String sourceQuality,
        SourceSummary sourceSummary,
        int version,
        OffsetDateTime generatedAt,
        OffsetDateTime savedAt,
        Error error
) {
    public static ResponseReflectionDetail from(Reflection reflection) {
        return new ResponseReflectionDetail(
                reflection.id(),
                reflection.kind().getDatabaseValue(),
                reflection.periodStart(),
                reflection.periodEnd(),
                reflection.title(),
                reflection.content(),
                reflection.status().getDatabaseValue(),
                reflection.sourceQuality().getDatabaseValue(),
                SourceSummary.from(reflection),
                reflection.version(),
                reflection.generatedAt(),
                reflection.savedAt(),
                Error.from(reflection.error())
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SourceSummary(
            String kind,
            Integer activityCount,
            Integer devlogCount,
            List<Evidence> evidence,
            Integer expectedDailyCount,
            Integer savedDailyCount,
            List<LocalDate> missingDailyDates,
            Integer fallbackActivityCount,
            List<DailyReflection> dailyReflections
    ) {
        private static SourceSummary from(Reflection reflection) {
            ReflectionSourceSnapshot source = reflection.sourceSnapshot();
            boolean daily = reflection.kind() == ReflectionKind.DAILY;
            return new SourceSummary(
                    daily ? null : reflection.kind().getDatabaseValue(),
                    daily ? source.activityCount() : null,
                    source.devlogCount(),
                    source.evidence().stream().map(Evidence::from).toList(),
                    daily ? null : source.expectedDailyCount(),
                    daily ? null : source.savedDailyCount(),
                    daily ? null : source.missingDailyDates(),
                    daily ? null : source.fallbackActivityCount(),
                    daily ? null : source.dailyReflections().stream()
                            .map(DailyReflection::from)
                            .toList()
            );
        }
    }

    public record Evidence(String ref, String title, String label) {
        private static Evidence from(ReflectionSourceSnapshot.Evidence evidence) {
            return new Evidence(evidence.ref(), evidence.title(), evidence.label());
        }
    }

    public record DailyReflection(
            Long reflectionId,
            LocalDate date,
            String title,
            String status,
            boolean included,
            String reason
    ) {
        private static DailyReflection from(
                ReflectionSourceSnapshot.DailyReflectionSource source
        ) {
            return new DailyReflection(
                    source.reflectionId(), source.date(), source.title(),
                    source.status(), source.included(), source.reason()
            );
        }
    }

    public record Error(String code, String message, boolean retryable) {
        public static Error from(Reflection.ReflectionError error) {
            return error == null ? null
                    : new Error(error.code(), error.message(), error.retryable());
        }
    }
}
