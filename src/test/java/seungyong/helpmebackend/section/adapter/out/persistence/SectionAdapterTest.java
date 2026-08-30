package seungyong.helpmebackend.section.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.section;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@Slf4j
@JpaTest
@PersistenceContext
public class SectionAdapterTest {
    @Autowired private SectionPortOut sectionPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("섹션 저장 - 성공")
    void save_section_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section = section(savedProject.getId());

        Section savedSection = sectionPortOut.save(section);

        assertThat(savedSection.getId()).isNotNull();
    }

    @Test
    @DisplayName("여러 섹션 저장 - 성공")
    void saveAll_sections_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section1 = section(null, savedProject.getId(), 1);
        Section section2 = section(null, savedProject.getId(), 2);

        var savedSections = sectionPortOut.saveAll(List.of(section1, section2));

        assertThat(savedSections).hasSize(2);
        assertThat(savedSections.get(0).getId()).isNotNull();
        assertThat(savedSections.get(1).getId()).isNotNull();
    }

    @Test
    @DisplayName("섹션 삭제 - 성공")
    void delete_section_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section = section(savedProject.getId());

        Section savedSection = sectionPortOut.save(section);
        assertThat(savedSection.getId()).isNotNull();

        sectionPortOut.delete(savedSection);

        Optional<Section> foundSection = sectionPortOut.getByIdAndUserId(savedSection.getId(), savedUser.getId());
        assertThat(foundSection).isEmpty();
    }

    @Test
    @DisplayName("모든 섹션 삭제 - 성공")
    void deleteAllByUserIdAndRepoFullName_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section1 = section(null, savedProject.getId(), 1);
        Section section2 = section(null, savedProject.getId(), 2);

        sectionPortOut.saveAll(List.of(section1, section2));

        sectionPortOut.deleteAllByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).isEmpty();
    }

    @Test
    @DisplayName("섹션 순서 감소 - 성공")
    void decreaseOrderIdxAfter_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section1 = section(null, savedProject.getId(), 2);

        sectionPortOut.save(section1);
        sectionPortOut.decreaseOrderIdxAfter(savedUser.getId(), savedProject.getRepoFullName(), 1);

        // 벌크 업데이트 후 영속성 컨텍스트 초기화
        entityManager.clear();

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).hasSize(1);
        assertThat(foundSections.get(0).getOrderIdx()).isEqualTo(1);
    }

    @Test
    @DisplayName("섹션 ID 및 유저 ID로 섹션 조회 - 성공")
    void getByIdAndUserId_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section = section(savedProject.getId());

        Section savedSection = sectionPortOut.save(section);

        Optional<Section> foundSection = sectionPortOut.getByIdAndUserId(savedSection.getId(), savedUser.getId());
        assertThat(foundSection).isPresent();
        assertThat(foundSection.get().getId()).isEqualTo(savedSection.getId());
    }

    @Test
    @DisplayName("섹션 ID 및 유저 ID로 섹션 조회 - 성공 (존재하지 않는 섹션)")
    void getByIdAndUserId_failure() {
        Optional<Section> foundSection = sectionPortOut.getByIdAndUserId(999L, 999L);
        assertThat(foundSection).isEmpty();
    }

    @Test
    @DisplayName("유저 ID 및 레포 이름으로 모든 섹션 조회 - 성공")
    void getSectionsByUserIdAndRepoFullName_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section1 = section(null, savedProject.getId(), 1);
        Section section2 = section(null, savedProject.getId(), 2);

        sectionPortOut.saveAll(List.of(section1, section2));

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).hasSize(2);
    }

    @Test
    @DisplayName("유저 ID 및 레포 이름으로 모든 섹션 조회 - 성공 (섹션이 없는 경우)")
    void getSectionsByUserIdAndRepoFullName_empty() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).isEmpty();
    }

    @Test
    @DisplayName("유저 ID 및 레포 이름으로 마지막 섹션 순서 조회 - 성공 (섹션이 없는 경우)")
    void lastOrderIdxByUserIdAndRepoFullName_success() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());

        Optional<Integer> lastOrderIdx = sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(lastOrderIdx).isEmpty();
    }

    @Test
    @DisplayName("유저 ID 및 레포 이름으로 마지막 섹션 순서 조회 - 성공 (섹션이 있는 경우)")
    void lastOrderIdxByUserIdAndRepoFullName_withSections() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section section1 = section(null, savedProject.getId(), 1);
        Section section2 = section(null, savedProject.getId(), 2);

        sectionPortOut.saveAll(List.of(section1, section2));

        Optional<Integer> lastOrderIdx = sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(lastOrderIdx).isNotEmpty()
                .hasValue(2);
    }

    @Test
    @DisplayName("컴포넌트 중간 삽입은 뒤 컴포넌트의 순서와 version을 함께 증가")
    void increaseOrderIdxFrom_versioned() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        sectionPortOut.saveAll(List.of(
                section(null, savedProject.getId(), 0),
                section(null, savedProject.getId(), 1)
        ));
        OffsetDateTime now = OffsetDateTime.parse("2026-08-30T10:00:00Z");

        assertThat(sectionPortOut.lockProject(savedProject.getId())).isTrue();
        sectionPortOut.increaseOrderIdxFrom(
                savedUser.getId(), savedProject.getRepoFullName(), 1, now
        );
        sectionPortOut.save(new Section(
                null, savedProject.getId(), "삽입", "본문", 1
        ));

        List<Section> components = sectionPortOut
                .getSectionsByUserIdAndRepoFullName(
                        savedUser.getId(), savedProject.getRepoFullName()
                );
        assertThat(components).extracting(Section::getOrderIdx)
                .containsExactly(0, 1, 2);
        assertThat(components.get(2).getVersion()).isEqualTo(1);
        assertThat(components.get(2).getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("컴포넌트 이동은 대상 version 조건으로 갱신하고 사이 항목을 재정렬")
    void updateIfVersionMatches_reorder() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        List<Section> saved = sectionPortOut.saveAll(List.of(
                section(null, savedProject.getId(), 0),
                section(null, savedProject.getId(), 1),
                section(null, savedProject.getId(), 2)
        ));
        Section target = saved.get(2);
        OffsetDateTime now = OffsetDateTime.parse("2026-08-30T10:10:00Z");

        assertThat(sectionPortOut.lockProject(savedProject.getId())).isTrue();
        sectionPortOut.shiftOrderIdxForMove(
                savedUser.getId(), savedProject.getRepoFullName(),
                target.getId(), 2, 0, now
        );
        Optional<Section> updated = sectionPortOut.updateIfVersionMatches(
                target.getId(), savedUser.getId(), savedProject.getRepoFullName(),
                "이동", "수정 본문", 0, 0, now
        );

        assertThat(updated).isPresent();
        assertThat(updated.orElseThrow().getVersion()).isEqualTo(1);
        List<Section> components = sectionPortOut
                .getSectionsByUserIdAndRepoFullName(
                        savedUser.getId(), savedProject.getRepoFullName()
                );
        assertThat(components).extracting(Section::getOrderIdx)
                .containsExactly(0, 1, 2);
        assertThat(components.get(0).getId()).isEqualTo(target.getId());
        assertThat(components).extracting(Section::getVersion)
                .containsExactly(1, 1, 1);
    }

    @Test
    @DisplayName("오래된 version으로는 컴포넌트를 수정하거나 삭제할 수 없음")
    void conditionalMutation_versionConflict() {
        User savedUser = saveUser();
        Project savedProject = saveProject(savedUser.getId());
        Section saved = sectionPortOut.save(section(savedProject.getId()));
        OffsetDateTime now = OffsetDateTime.parse("2026-08-30T10:20:00Z");

        assertThat(sectionPortOut.updateIfVersionMatches(
                saved.getId(), savedUser.getId(), savedProject.getRepoFullName(),
                "오래된 수정", "본문", 1, 3, now
        )).isEmpty();
        assertThat(sectionPortOut.deleteIfVersionMatches(
                saved.getId(), savedUser.getId(), savedProject.getRepoFullName(), 3
        )).isFalse();
    }

    private User saveUser() {
        return userPortOut.save(user(null, "test-token"));
    }

    private Project saveProject(Long userId) {
        return projectPortOut.save(project(userId));
    }
}
