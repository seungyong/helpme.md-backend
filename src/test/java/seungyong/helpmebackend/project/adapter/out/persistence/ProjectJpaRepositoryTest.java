package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.projectJpaEntity;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@Slf4j
@JpaTest
public class ProjectJpaRepositoryTest {
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectJpaRepository projectJpaRepository;

    @Test
    @DisplayName("유저 ID와 레포지토리 이름으로 조회 - 성공")
    void findByUserIdAndRepoFullName_success() {
        User user = user();

        User savedUser = userPortOut.save(user);
        UserJpaEntity userJpaEntity = UserPersistenceMapper.INSTANCE.toJpaEntity(savedUser);

        ProjectJpaEntity project = projectJpaEntity(userJpaEntity);

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
