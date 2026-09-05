package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.activity.adapter.out.persistence.entity.ActivityJpaEntity;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioSourcePortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceData;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.reflection.adapter.out.persistence.entity.ReflectionJpaEntity;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PortfolioSourceAdapter implements PortfolioSourcePortOut {
    private final PortfolioReflectionSourceJpaRepository reflectionRepository;
    private final PortfolioActivitySourceJpaRepository activityRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PortfolioSourceData findCandidates(Long projectId, LocalDate periodStart, LocalDate periodEnd,
                                              OffsetDateTime activityFrom, OffsetDateTime activityTo) {
        List<ReflectionJpaEntity> reflections = reflectionRepository.findCandidates(
                projectId, ReflectionStatus.SAVED, periodStart, periodEnd
        );
        List<ActivityJpaEntity> activities = activityRepository.findCandidates(
                projectId, activityFrom, activityTo
        );
        return toData(reflections, activities);
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioSourceData findSelected(Long projectId, List<Long> reflectionIds, List<Long> activityIds) {
        List<ReflectionJpaEntity> reflections = reflectionIds.isEmpty() ? List.of()
                : reflectionRepository.findSelected(projectId, ReflectionStatus.SAVED, reflectionIds);
        List<ActivityJpaEntity> activities = activityIds.isEmpty() ? List.of()
                : activityRepository.findSelected(projectId, activityIds);
        return toData(reflections, activities);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSavedReflections(Long projectId) {
        return reflectionRepository.countByProject_IdAndStatus(projectId, ReflectionStatus.SAVED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean reflectionVersionsMatch(
            Long projectId, List<PortfolioSourceSnapshot.ReflectionSource> reflections
    ) {
        if (reflections.isEmpty()) {
            return true;
        }

        List<Long> ids = reflections.stream().map(PortfolioSourceSnapshot.ReflectionSource::id).toList();
        List<ReflectionJpaEntity> current = reflectionRepository.findSelected(
                projectId, ReflectionStatus.SAVED, ids
        );

        if (current.size() != reflections.size()) {
            return false;
        }

        return reflections.stream().allMatch(source -> current.stream().anyMatch(entity ->
                entity.getId().equals(source.id()) && entity.getVersion() == source.version()
        ));
    }

    private PortfolioSourceData toData(List<ReflectionJpaEntity> reflections, List<ActivityJpaEntity> activities) {
        return new PortfolioSourceData(
                reflections.stream().map(this::toReflection).toList(),
                activities.stream().map(this::toActivity).toList()
        );
    }

    private PortfolioSourceData.ReflectionData toReflection(ReflectionJpaEntity entity) {
        return new PortfolioSourceData.ReflectionData(
                entity.getId(), entity.getKind(), entity.getPeriodStart(), entity.getPeriodEnd(),
                entity.getTitle(), entity.getVersion(), readDocument(entity.getContent())
        );
    }

    private PortfolioSourceData.ActivityData toActivity(ActivityJpaEntity entity) {
        return new PortfolioSourceData.ActivityData(
                entity.getId(), entity.getActivityType(), entity.getBranchName(), entity.getCommitSha(),
                entity.getTitle(), entity.getPublicUrl()
        );
    }

    private ReflectionDocument readDocument(JsonNode node) {
        try {
            if (node != null && node.has("schemaVersion")) {
                return objectMapper.treeToValue(node, ReflectionDocument.class);
            }
            List<ReflectionDocument.Section> sections = new ArrayList<>();
            if (node != null && node.path("sections").isArray()) {
                for (JsonNode section : node.path("sections")) {
                    sections.add(objectMapper.treeToValue(section, ReflectionDocument.Section.class));
                }
            }
            return new ReflectionDocument(ReflectionDocument.CURRENT_SCHEMA_VERSION, sections);
        } catch (Exception exception) {
            throw new IllegalStateException("invalid portfolio reflection source", exception);
        }
    }
}
