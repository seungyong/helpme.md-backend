package seungyong.helpmebackend.portfolio.application.port.out;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceData;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface PortfolioSourcePortOut {
    PortfolioSourceData findCandidates(Long projectId, LocalDate periodStart, LocalDate periodEnd,
                                       OffsetDateTime activityFrom, OffsetDateTime activityTo);

    PortfolioSourceData findSelected(Long projectId, List<Long> reflectionIds, List<Long> activityIds);

    long countSavedReflections(Long projectId);

    boolean reflectionVersionsMatch(Long projectId, List<PortfolioSourceSnapshot.ReflectionSource> reflections);
}
