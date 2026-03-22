package seungyong.helpmebackend.project.adapter.out.persistence;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.BuilderArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOutMapper;
import seungyong.helpmebackend.user.domain.entity.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@JpaTest
public class ProjectJpaRepositoryTest {
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectJpaRepository projectJpaRepository;

    @Test
    @DisplayName("유저 ID와 레포지토리 이름으로 조회 - 성공")
    void findByUserIdAndRepoFullName_success() {
        User user = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(User.class)
                .setNull("id")
                .sample();

        User savedUser = userPortOut.save(user);
        UserJpaEntity userJpaEntity = UserPortOutMapper.INSTANCE.toJpaEntity(savedUser);

        ProjectJpaEntity project = FixtureMonkey.builder()
                .objectIntrospector(BuilderArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(ProjectJpaEntity.class)
                .setNull("id")
                .set("user", userJpaEntity)
                .sample();

        assert user != null;
        assert project != null;

        projectJpaRepository.save(project);

        Optional<ProjectJpaEntity> foundProject = projectJpaRepository.findByUser_IdAndRepoFullName(
                project.getUser().getId(), project.getRepoFullName()
        );

        assertThat(foundProject)
                .isPresent()
                .get()
                .satisfies(p -> {
                    assertThat(p.getId()).isNotNull();
                    assertThat(p.getUser().getId()).isEqualTo(userJpaEntity.getId());
                    assertThat(p.getRepoFullName()).isEqualTo(project.getRepoFullName());
                });
    }
}
