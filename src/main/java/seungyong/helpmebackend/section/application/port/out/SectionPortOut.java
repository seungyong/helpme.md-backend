package seungyong.helpmebackend.section.application.port.out;

import seungyong.helpmebackend.section.domain.entity.Section;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SectionPortOut {
    Section save(Section section);
    List<Section> saveAll(List<Section> sections);
    void delete(Section section);
    void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName);

    void decreaseOrderIdxAfter(Long userId, String repoFullName, Integer targetIdx);

    Optional<Section> getByIdAndUserId(Long sectionId, Long userId);
    Optional<Section> getByIdAndUserIdAndRepoFullName(
            Long sectionId,
            Long userId,
            String repoFullName
    );
    List<Section> getSectionsByUserIdAndRepoFullName(Long userId, String repoFullName);
    Optional<Integer> lastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName);

    boolean lockProject(Long projectId);

    void increaseOrderIdxFrom(
            Long userId,
            String repoFullName,
            Integer targetIdx,
            OffsetDateTime updatedAt
    );

    void shiftOrderIdxForMove(
            Long userId,
            String repoFullName,
            Long sectionId,
            Integer currentIdx,
            Integer targetIdx,
            OffsetDateTime updatedAt
    );

    Optional<Section> updateIfVersionMatches(
            Long sectionId,
            Long userId,
            String repoFullName,
            String title,
            String content,
            Integer orderIdx,
            Integer expectedVersion,
            OffsetDateTime updatedAt
    );

    boolean deleteIfVersionMatches(
            Long sectionId,
            Long userId,
            String repoFullName,
            Integer expectedVersion
    );

    void decreaseOrderIdxAfterVersioned(
            Long userId,
            String repoFullName,
            Integer targetIdx,
            OffsetDateTime updatedAt
    );
}
