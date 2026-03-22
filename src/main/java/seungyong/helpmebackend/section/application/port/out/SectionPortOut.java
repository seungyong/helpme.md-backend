package seungyong.helpmebackend.section.application.port.out;

import seungyong.helpmebackend.section.domain.entity.Section;

import java.util.List;
import java.util.Optional;

public interface SectionPortOut {
    Section save(Section section);
    List<Section> saveAll(List<Section> sections);
    void delete(Section section);
    void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName);

    void decreaseOrderIdxAfter(Long userId, String repoFullName, Integer targetIdx);

    Optional<Section> getByIdAndUserId(Long sectionId, Long userId);
    List<Section> getSectionsByUserIdAndRepoFullName(Long userId, String repoFullName);
    Optional<Integer> lastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName);
}
