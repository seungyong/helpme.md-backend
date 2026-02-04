package seungyong.helpmebackend.usecase.port.out.section;

import seungyong.helpmebackend.domain.entity.section.Section;

import java.util.List;

public interface SectionPortOut {
    Section save(Section section);
    List<Section> saveAll(List<Section> sections);
    void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName);

    List<Section> getSectionsByUserIdAndRepoFullName(Long userId, String repoFullName);
    Short lastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName);
}
