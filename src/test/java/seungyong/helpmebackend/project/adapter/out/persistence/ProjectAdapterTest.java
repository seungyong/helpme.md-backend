package seungyong.helpmebackend.project.adapter.out.persistence;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@JpaTest
public class ProjectAdapterTest {
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Test
    @DisplayName("프로젝트 저장 - 성공")
    void save_project_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();
        assert user != null;

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();
        assert project != null;

        Project savedProject = projectPortOut.save(project);

        assertThat(savedProject.getId()).isNotNull();
    }

    @Test
    @DisplayName("유저 ID 및 이름으로 프로젝트 조회 - 성공")
    void getByUserIdAndRepoFullName_success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();
        assert user != null;

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();
        assert project != null;

        Project savedProject = projectPortOut.save(project);

        assertThat(savedProject.getId()).isNotNull();

        Project foundProject = projectPortOut.getByUserIdAndRepoFullName(savedUser.getId(), project.getRepoFullName())
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다."));

        assertThat(foundProject.getId()).isEqualTo(savedProject.getId());
    }

    @Test
    @DisplayName("유저 ID 및 이름으로 프로젝트 조회 - 실패")
    void getByUserIdAndRepoFullName_failure() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test-token")
                .sample();
        assert user != null;

        User savedUser = userPortOut.save(user);

        Project project = fixtureMonkey.giveMeBuilder(Project.class)
                .setNull("id")
                .set("userId", savedUser.getId())
                .sample();
        assert project != null;

        projectPortOut.save(project);

        assertThat(projectPortOut.getByUserIdAndRepoFullName(savedUser.getId(), "nonexistent/repo")).isEmpty();
    }
}
