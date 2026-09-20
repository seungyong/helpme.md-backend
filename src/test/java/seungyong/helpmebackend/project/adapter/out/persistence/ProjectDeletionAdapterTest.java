package seungyong.helpmebackend.project.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class ProjectDeletionAdapterTest {
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private ProjectDeletionPortOut deletionPortOut;

    @Test
    @DisplayName("프로젝트 삭제 요청을 한 worker만 lease로 claim하고 hard delete")
    void claimsOnceAndHardDeletes() {
        var savedUser = userPortOut.save(user(null, "project-deletion-user-token"));
        Project saved = projectPortOut.save(project(savedUser.getId()));
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T09:00:00Z");
        Project deleting = deletionPortOut.requestDeletion(saved.getId(), requestedAt);
        assertThat(deleting.getStatus()).isEqualTo(ProjectStatus.DELETING);

        OffsetDateTime claimedAt = OffsetDateTime.now().plusMinutes(10);
        assertThat(deletionPortOut.claimNext(
                claimedAt, claimedAt.minusMinutes(5), claimedAt.minusMinutes(1)
        )).isPresent();
        assertThat(deletionPortOut.claimNext(
                claimedAt, claimedAt.minusMinutes(5), claimedAt.minusMinutes(1)
        )).isEmpty();

        deletionPortOut.hardDelete(saved.getId());
        assertThat(projectPortOut.getById(saved.getId())).isEmpty();
    }
}
