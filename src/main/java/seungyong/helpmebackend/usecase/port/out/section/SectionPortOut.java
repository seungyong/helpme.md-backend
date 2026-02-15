package seungyong.helpmebackend.usecase.port.out.section;

import seungyong.helpmebackend.domain.entity.section.Section;

import java.util.List;
import java.util.Optional;

public interface SectionPortOut {
    Section save(Section section);
    List<Section> saveAll(List<Section> sections);
    void delete(Section section);
    void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName);

    void decreaseOrderIdxAfter(Long userId, String repoFullName, Short targetIdx);

    Optional<Section> getByIdAndUserId(Long sectionId, Long userId);
    List<Section> getSectionsByUserIdAndRepoFullName(Long userId, String repoFullName);
    Short lastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName);
}
