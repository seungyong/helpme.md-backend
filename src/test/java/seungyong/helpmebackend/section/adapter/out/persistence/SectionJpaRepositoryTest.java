package seungyong.helpmebackend.section.adapter.out.persistence;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOutMapper;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.section.adapter.out.persistence.entity.SectionJpaEntity;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@JpaTest
@PersistenceContext
public class SectionJpaRepositoryTest {
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private SectionJpaRepository sectionJpaRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("특정 프로젝트의 모든 섹션 조회 - 성공")
    void findAllByUserIdAndRepoFullName_Success() {
        User user = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);
        ProjectJpaEntity savedProjectEntity = ProjectPortOutMapper.INSTANCE.toJpaEntity(savedProject);

        SectionJpaEntity section1 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 1")
                .content("Content for section 1")
                .orderIdx(1)
                .build();

        SectionJpaEntity section2 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 2")
                .content("Content for section 2")
                .orderIdx(2)
                .build();

        sectionJpaRepository.saveAll(List.of(section1, section2));

        List<SectionJpaEntity> sections = sectionJpaRepository.findAllByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(sections)
                .isNotEmpty()
                .hasSize(2)
                .extracting(SectionJpaEntity::getId)
                .doesNotContainNull();
    }

    @Test
    @DisplayName("특정 프로젝트의 모든 섹션 조회 - 성공 (섹션이 없는 경우)")
    void findAllByUserIdAndRepoFullName_EmptyList() {
        List<SectionJpaEntity> sections = sectionJpaRepository.findAllByUserIdAndRepoFullName(
                999L, // 존재하지 않는 유저 ID
                "nonexistent/repo" // 존재하지 않는 레포 이름
        );
        assertThat(sections).isEmpty();
    }

    @Test
    @DisplayName("특정 프로젝트의 마지막 섹션 조회 - 성공")
    void findLastOrderIdxByUserIdAndRepoFullName_Success() {
        User user = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);
        ProjectJpaEntity savedProjectEntity = ProjectPortOutMapper.INSTANCE.toJpaEntity(savedProject);

        SectionJpaEntity section1 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 1")
                .content("Content for section 1")
                .orderIdx(1)
                .build();

        SectionJpaEntity section2 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 2")
                .content("Content for section 2")
                .orderIdx(2)
                .build();

        sectionJpaRepository.saveAll(List.of(section1, section2));

        SectionJpaEntity lastSection = sectionJpaRepository.findLastOrderIdxByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName())
                .orElseThrow(() -> new RuntimeException("마지막 섹션을 찾을 수 없습니다."));

        assertThat(lastSection.getTitle()).isEqualTo("Section 2");
        assertThat(lastSection.getOrderIdx()).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 프로젝트의 마지막 섹션 조회 - 성공 (섹션이 없는 경우)")
    void findLastOrderIdxByUserIdAndRepoFullName_Empty() {
        Optional<SectionJpaEntity> section = sectionJpaRepository.findLastOrderIdxByUserIdAndRepoFullName(
                999L, // 존재하지 않는 유저 ID
                "nonexistent/repo" // 존재하지 않는 레포 이름
        );
        assertThat(section).isEmpty();
    }

    @Test
    @DisplayName("특정 프로젝트의 섹션 조회 - 성공")
    void findByIdAndProject_User_Id_Success() {
        User user = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);
        ProjectJpaEntity savedProjectEntity = ProjectPortOutMapper.INSTANCE.toJpaEntity(savedProject);

        SectionJpaEntity section = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 1")
                .content("Content for section 1")
                .orderIdx(1)
                .build();

        SectionJpaEntity savedSection = sectionJpaRepository.save(section);

        Optional<SectionJpaEntity> foundSection = sectionJpaRepository.findByIdAndProject_User_Id(savedSection.getId(), savedUser.getId());
        assertThat(foundSection).isPresent();
        assertThat(foundSection.get().getId()).isEqualTo(savedSection.getId());
    }

    @Test
    @DisplayName("특정 프로젝트의 섹션 조회 - 성공 (섹션이 없는 경우)")
    void findByIdAndProject_User_Id_Empty() {
        Optional<SectionJpaEntity> section = sectionJpaRepository.findByIdAndProject_User_Id(
                999L, // 존재하지 않는 섹션 ID
                999L  // 존재하지 않는 유저 ID
        );
        assertThat(section).isEmpty();
    }

    @Test
    @DisplayName("모든 섹션 삭제 - 성공")
    void deleteAllByUserIdAndRepoFullName_Success() {
        User user = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);
        ProjectJpaEntity savedProjectEntity = ProjectPortOutMapper.INSTANCE.toJpaEntity(savedProject);

        SectionJpaEntity section1 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 1")
                .content("Content for section 1")
                .orderIdx(1)
                .build();

        SectionJpaEntity section2 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 2")
                .content("Content for section 2")
                .orderIdx(2)
                .build();

        sectionJpaRepository.saveAll(List.of(section1, section2));

        sectionJpaRepository.deleteAllByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());

        List<SectionJpaEntity> sections = sectionJpaRepository.findAllByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(sections).isEmpty();
    }

    @Test
    @DisplayName("섹션 순서 감소 - 성공")
    void decreaseOrderIdxAfter_Success() {
        User user = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();

        User savedUser = userPortOut.save(user);

        Project project = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();

        Project savedProject = projectPortOut.save(project);
        ProjectJpaEntity savedProjectEntity = ProjectPortOutMapper.INSTANCE.toJpaEntity(savedProject);

        SectionJpaEntity section1 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 1")
                .content("Content for section 1")
                .orderIdx(1)
                .build();

        SectionJpaEntity section2 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 2")
                .content("Content for section 2")
                .orderIdx(2)
                .build();

        SectionJpaEntity section3 = SectionJpaEntity.builder()
                .project(savedProjectEntity)
                .title("Section 3")
                .content("Content for section 3")
                .orderIdx(3)
                .build();

        sectionJpaRepository.saveAll(List.of(section1, section2, section3));
        sectionJpaRepository.delete(section2);

        // orderIdx가 2보다 큰 섹션들의 orderIdx를 감소
        sectionJpaRepository.decreaseOrderIdxAfter(savedUser.getId(), savedProject.getRepoFullName(), section2.getOrderIdx());

        // 벌크 업데이트 후 영속성 컨텍스트를 초기화하여 변경된 데이터를 DB에서 다시 조회
        entityManager.clear();

        List<SectionJpaEntity> sections = sectionJpaRepository.findAllByUserIdAndRepoFullName(savedUser.getId(), savedProject.getRepoFullName());
        assertThat(sections)
                .hasSize(2)
                .extracting(SectionJpaEntity::getOrderIdx)
                .containsExactly(1, 2); // section3의 orderIdx가 3에서 2로 감소
    }
}
