package seungyong.helpmebackend.section.adapter.out.persistence;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@JpaTest
@PersistenceContext
public class SectionAdapterTest {
    @Autowired private SectionPortOut sectionPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private EntityManager entityManager;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Test
    @DisplayName("섹션 저장 - 성공")
    void save_section_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        Section savedSection = sectionPortOut.save(section);

        assertThat(savedSection.getId()).isNotNull();
    }

    @Test
    @DisplayName("여러 섹션 저장 - 성공")
    void saveAll_sections_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section1 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        Section section2 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        assert section1 != null;
        assert section2 != null;

        var savedSections = sectionPortOut.saveAll(List.of(section1, section2));

        assertThat(savedSections).hasSize(2);
        assertThat(savedSections.get(0).getId()).isNotNull();
        assertThat(savedSections.get(1).getId()).isNotNull();
    }

    @Test
    @DisplayName("섹션 삭제 - 성공")
    void delete_section_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        Section savedSection = sectionPortOut.save(section);
        assertThat(savedSection.getId()).isNotNull();

        sectionPortOut.delete(savedSection);

        Optional<Section> foundSection = sectionPortOut.getByIdAndUserId(savedSection.getId(), savedUser.getId());
        assertThat(foundSection).isEmpty();
    }

    @Test
    @DisplayName("모든 섹션 삭제 - 성공")
    void deleteAllByUserIdAndRepoFullName_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section1 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        Section section2 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        assert section1 != null;
        assert section2 != null;

        sectionPortOut.saveAll(List.of(section1, section2));

        sectionPortOut.deleteAllByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).isEmpty();
    }

    @Test
    @DisplayName("섹션 순서 감소 - 성공")
    void decreaseOrderIdxAfter_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section1 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .set("orderIdx", (short) 2)
                .sample();

        assert section1 != null;

        sectionPortOut.save(section1);
        sectionPortOut.decreaseOrderIdxAfter(savedUser.getId(), savedProject.getRepoFullName(), (short) 1);

        // 벌크 업데이트 후 영속성 컨텍스트 초기화
        entityManager.clear();

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).hasSize(1);
        assertThat(foundSections.get(0).getOrderIdx()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("섹션 ID 및 유저 ID로 섹션 조회 - 성공")
    void getByIdAndUserId_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

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
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section1 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        Section section2 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .sample();

        assert section1 != null;
        assert section2 != null;

        sectionPortOut.saveAll(List.of(section1, section2));

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).hasSize(2);
    }

    @Test
    @DisplayName("유저 ID 및 레포 이름으로 모든 섹션 조회 - 성공 (섹션이 없는 경우)")
    void getSectionsByUserIdAndRepoFullName_empty() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        List<Section> foundSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(foundSections).isEmpty();
    }

     @Test
    @DisplayName("유저 ID 및 레포 이름으로 마지막 섹션 순서 조회 - 성공 (섹션이 없는 경우)")
    void lastOrderIdxByUserIdAndRepoFullName_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Short lastOrderIdx = sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(lastOrderIdx).isEqualTo((short) 0);
    }

    @Test
    @DisplayName("유저 ID 및 레포 이름으로 마지막 섹션 순서 조회 - 성공 (섹션이 있는 경우)")
    void lastOrderIdxByUserIdAndRepoFullName_withSections() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);

        Section section1 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .set("orderIdx", (short) 1)
                .sample();

        Section section2 = fixtureMonkey.giveMeBuilder(Section.class)
                .setNull("id")
                .set("projectId", savedProject.getId())
                .set("orderIdx", (short) 2)
                .sample();

        assert section1 != null;
        assert section2 != null;

        sectionPortOut.saveAll(List.of(section1, section2));

        Short lastOrderIdx = sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(lastOrderIdx).isEqualTo((short) 2);
    }
}
