package seungyong.helpmebackend.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.adapter.out.persistence.entity.SectionJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.mapper.SectionPortOutMapper;
import seungyong.helpmebackend.adapter.out.persistence.repository.SectionJpaRepository;
import seungyong.helpmebackend.domain.entity.section.Section;
import seungyong.helpmebackend.usecase.port.out.section.SectionPortOut;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SectionAdapter implements SectionPortOut {
    private final SectionJpaRepository sectionJpaRepository;

    @Override
    public Section save(Section section) {
        SectionJpaEntity savedEntity = sectionJpaRepository.save(SectionPortOutMapper.INSTANCE.toEntity(section));
        return SectionPortOutMapper.INSTANCE.toDomain(savedEntity);
    }

    @Override
    public List<Section> saveAll(List<Section> sections) {
        List<SectionJpaEntity> sectionJpaEntities = sections.stream()
                .map(SectionPortOutMapper.INSTANCE::toEntity)
                .toList();

        return sectionJpaRepository.saveAll(sectionJpaEntities)
                .stream()
                .map(SectionPortOutMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public void delete(Section section) {
        sectionJpaRepository.delete(SectionPortOutMapper.INSTANCE.toEntity(section));
    }

    @Override
    public void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName) {
        sectionJpaRepository.deleteAllByUserIdAndRepoFullName(userId, repoFullName);
    }

    @Override
    public void decreaseOrderIdxAfter(Long userId, String repoFullName, Short targetIdx) {
        sectionJpaRepository.decreaseOrderIdxAfter(userId, repoFullName, targetIdx);
    }

    @Override
    public Optional<Section> getByIdAndUserId(Long sectionId, Long userId) {
        return sectionJpaRepository.findByIdAndProject_User_Id(sectionId, userId)
                .map(SectionPortOutMapper.INSTANCE::toDomain);
    }

    @Override
    public List<Section> getSectionsByUserIdAndRepoFullName(Long userId, String repoFullName) {
        return sectionJpaRepository.findAllByUserIdAndRepoFullName(userId, repoFullName)
                .stream()
                .map(SectionPortOutMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public Short lastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName) {
        return sectionJpaRepository.findLastOrderIdxByUserIdAndRepoFullName(userId, repoFullName)
                .map(SectionJpaEntity::getOrderIdx)
                .orElse((short) 0);
    }
}
