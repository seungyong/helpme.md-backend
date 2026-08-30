package seungyong.helpmebackend.section.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.section.adapter.out.persistence.entity.SectionJpaEntity;
import seungyong.helpmebackend.section.adapter.out.persistence.mapper.SectionPersistenceMapper;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SectionAdapter implements SectionPortOut {
    private final SectionJpaRepository sectionJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Section save(Section section) {
        SectionJpaEntity savedEntity = sectionJpaRepository.save(
                SectionPersistenceMapper.toEntity(section)
        );
        return SectionPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public List<Section> saveAll(List<Section> sections) {
        List<SectionJpaEntity> sectionJpaEntities = sections.stream()
                .map(SectionPersistenceMapper::toEntity)
                .toList();

        return sectionJpaRepository.saveAll(sectionJpaEntities)
                .stream()
                .map(SectionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Section section) {
        sectionJpaRepository.delete(SectionPersistenceMapper.toEntity(section));
    }

    /**
     * 특정 유저의 특정 저장소에 속한 모든 섹션을 삭제합니다.
     * <br /> <br />
     * 주의 : 벌크 삭제이므로, 영속성 컨텍스트를 거치지 않고 바로 DB에 반영됩니다.
     * 따라서 이 메서드를 호출한 후에는 영속성 컨텍스트를 명시적으로 초기화하거나, 필요한 경우 섹션 엔티티들을 다시 조회해야 합니다.
     *
     * @param userId        유저 ID
     * @param repoFullName  저장소 전체 이름 (예: "owner/repo")
     */
    @Override
    public void deleteAllByUserIdAndRepoFullName(Long userId, String repoFullName) {
        sectionJpaRepository.deleteAllByUserIdAndRepoFullName(userId, repoFullName);
    }

    /**
     * 특정 섹션을 삭제한 후, 해당 섹션보다 orderIdx가 큰 섹션들의 orderIdx를 1씩 감소시킵니다.
     * <br /> <br />
     * 주의 : 벌크 업데이트이므로, 영속성 컨텍스트를 거치지 않고 바로 DB에 반영됩니다.
     * 따라서 이 메서드를 호출한 후에는 영속성 컨텍스트를 명시적으로 초기화하거나, 필요한 경우 섹션 엔티티들을 다시 조회해야 합니다.
     *
     * @param userId        유저 ID
     * @param repoFullName  저장소 전체 이름 (예: "owner/repo")
     * @param targetIdx     삭제된 섹션의 orderIdx
     */
    @Override
    public void decreaseOrderIdxAfter(Long userId, String repoFullName, Integer targetIdx) {
        sectionJpaRepository.decreaseOrderIdxAfter(userId, repoFullName, targetIdx);
    }

    @Override
    public Optional<Section> getByIdAndUserId(Long sectionId, Long userId) {
        return sectionJpaRepository.findByIdAndProject_User_Id(sectionId, userId)
                .map(SectionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Section> getByIdAndUserIdAndRepoFullName(
            Long sectionId,
            Long userId,
            String repoFullName
    ) {
        return sectionJpaRepository
                .findByIdAndProject_User_IdAndProject_RepoFullName(
                        sectionId, userId, repoFullName
                )
                .map(SectionPersistenceMapper::toDomain);
    }

    @Override
    public List<Section> getSectionsByUserIdAndRepoFullName(Long userId, String repoFullName) {
        return sectionJpaRepository.findAllByUserIdAndRepoFullName(userId, repoFullName)
                .stream()
                .map(SectionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Integer> lastOrderIdxByUserIdAndRepoFullName(Long userId, String repoFullName) {
        return sectionJpaRepository.findLastOrderIdxByUserIdAndRepoFullName(userId, repoFullName)
                .map(SectionJpaEntity::getOrderIdx);
    }

    @Override
    public boolean lockProject(Long projectId) {
        return entityManager.find(
                ProjectJpaEntity.class,
                projectId,
                LockModeType.PESSIMISTIC_WRITE
        ) != null;
    }

    @Override
    public void increaseOrderIdxFrom(
            Long userId,
            String repoFullName,
            Integer targetIdx,
            OffsetDateTime updatedAt
    ) {
        sectionJpaRepository.increaseOrderIdxFrom(
                userId, repoFullName, targetIdx, updatedAt
        );
    }

    @Override
    public void shiftOrderIdxForMove(
            Long userId,
            String repoFullName,
            Long sectionId,
            Integer currentIdx,
            Integer targetIdx,
            OffsetDateTime updatedAt
    ) {
        if (currentIdx < targetIdx) {
            sectionJpaRepository.shiftOrderIdxDownForMove(
                    userId, repoFullName, sectionId, currentIdx, targetIdx, updatedAt
            );
        } else if (currentIdx > targetIdx) {
            sectionJpaRepository.shiftOrderIdxUpForMove(
                    userId, repoFullName, sectionId, currentIdx, targetIdx, updatedAt
            );
        }
    }

    @Override
    @Transactional
    public Optional<Section> updateIfVersionMatches(
            Long sectionId,
            Long userId,
            String repoFullName,
            String title,
            String content,
            Integer orderIdx,
            Integer expectedVersion,
            OffsetDateTime updatedAt
    ) {
        int changed = sectionJpaRepository.updateIfVersionMatches(
                sectionId, userId, repoFullName, title, content, orderIdx,
                expectedVersion, updatedAt
        );
        if (changed == 0) {
            return Optional.empty();
        }
        return getByIdAndUserIdAndRepoFullName(sectionId, userId, repoFullName);
    }

    @Override
    public boolean deleteIfVersionMatches(
            Long sectionId,
            Long userId,
            String repoFullName,
            Integer expectedVersion
    ) {
        return sectionJpaRepository.deleteIfVersionMatches(
                sectionId, userId, repoFullName, expectedVersion
        ) == 1;
    }

    @Override
    public void decreaseOrderIdxAfterVersioned(
            Long userId,
            String repoFullName,
            Integer targetIdx,
            OffsetDateTime updatedAt
    ) {
        sectionJpaRepository.decreaseOrderIdxAfterVersioned(
                userId, repoFullName, targetIdx, updatedAt
        );
    }
}
