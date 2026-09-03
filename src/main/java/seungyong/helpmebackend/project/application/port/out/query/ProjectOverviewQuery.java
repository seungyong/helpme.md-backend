package seungyong.helpmebackend.project.application.port.out.query;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProjectOverviewQuery(
        OffsetDateTime previousFrom,
        OffsetDateTime currentFrom,
        OffsetDateTime currentTo,
        OffsetDateTime todayFrom,
        OffsetDateTime todayTo,
        LocalDate previousPeriodFrom,
        LocalDate currentPeriodFrom,
        LocalDate currentPeriodTo,
        LocalDate weekStart,
        LocalDate weekEnd,
        LocalDate today
) {
}
