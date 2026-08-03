package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import static org.assertj.core.api.Assertions.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@Slf4j
@JpaTest
public class ProjectAdapterTest {
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectJpaRepository projectJpaRepository;

    @Test
    @DisplayName("프로젝트 저장 - 성공")
    void save_project_success() {
        User user = user(null, "test-token");

        User savedUser = userPortOut.save(user);

        Project project = project(savedUser.getId());

        Project savedProject = projectPortOut.save(project);

        assertThat(savedProject.getId()).isNotNull();
    }

    @Test
    @DisplayName("유저 ID 및 이름으로 프로젝트 조회 - 성공")
    void getByUserIdAndRepoFullName_success() {
        User user = user(null, "test-token");

        User savedUser = userPortOut.save(user);

        Project project = project(savedUser.getId());

        Project savedProject = projectPortOut.save(project);

        assertThat(savedProject.getId()).isNotNull();

        Project foundProject = projectPortOut.getByUserIdAndRepoFullName(savedUser.getId(), project.getRepoFullName())
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다."));

        assertThat(foundProject.getId()).isEqualTo(savedProject.getId());
    }

    @Test
    @DisplayName("유저 ID 및 이름으로 프로젝트 조회 - 실패")
    void getByUserIdAndRepoFullName_failure() {
        User user = user(null, "test-token");

        User savedUser = userPortOut.save(user);

        Project project = project(savedUser.getId());

        projectPortOut.save(project);

        assertThat(projectPortOut.getByUserIdAndRepoFullName(savedUser.getId(), "nonexistent/repo")).isEmpty();
    }

    @Test
    @DisplayName("현재 사용자가 이미 연결한 GitHub Repository ID만 조회")
    void getConnectedGithubRepoIds_success() {
        User savedUser = userPortOut.save(user(null, "test-token"));
        var userJpaEntity = UserPersistenceMapper.INSTANCE.toJpaEntity(savedUser);
        projectJpaRepository.save(ProjectJpaEntity.builder()
                .user(userJpaEntity)
                .repoFullName("octocat/connected")
                .githubRepoId(101L)
                .build());

        assertThat(projectPortOut.getConnectedGithubRepoIds(
                savedUser.getId(),
                java.util.List.of(101L, 102L)
        )).containsExactly(101L);
        assertThat(projectPortOut.getConnectedGithubRepoIds(
                savedUser.getId(),
                java.util.List.of()
        )).isEmpty();
    }
}
