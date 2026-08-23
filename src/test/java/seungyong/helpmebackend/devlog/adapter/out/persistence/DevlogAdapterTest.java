package seungyong.helpmebackend.devlog.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class DevlogAdapterTest {
    @Autowired private DevlogPortOut devlogPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;

    @Test
    @DisplayName("최초 저장과 version 조건 수정·삭제를 DB 반영 건수로 보장")
    void versionConditionalWrite_successAndConflict() {
        User savedUser = userPortOut.save(user(null, "test-token"));
        Project savedProject = projectPortOut.save(project(savedUser.getId()));
        LocalDate logDate = LocalDate.of(2026, 8, 23);

        Devlog created = devlogPortOut.create(
                savedProject.getId(), logDate, "첫 개발로그"
        );

        assertThat(created.id()).isNotNull();
        assertThat(created.version()).isZero();
        assertThat(devlogPortOut.updateIfVersionMatches(
                savedProject.getId(), logDate, "잘못된 version", 1,
                OffsetDateTime.parse("2026-08-23T10:00:00Z")
        )).isEmpty();

        Devlog updated = devlogPortOut.updateIfVersionMatches(
                savedProject.getId(), logDate, "수정된 개발로그", 0,
                OffsetDateTime.parse("2026-08-23T10:01:00Z")
        ).orElseThrow();

        assertThat(updated.contentMarkdown()).isEqualTo("수정된 개발로그");
        assertThat(updated.version()).isEqualTo(1);
        assertThat(devlogPortOut.deleteIfVersionMatches(
                savedProject.getId(), logDate, 0
        )).isFalse();
        assertThat(devlogPortOut.deleteIfVersionMatches(
                savedProject.getId(), logDate, 1
        )).isTrue();
        assertThat(devlogPortOut.getByProjectIdAndLogDate(
                savedProject.getId(), logDate
        )).isEmpty();
    }
}
