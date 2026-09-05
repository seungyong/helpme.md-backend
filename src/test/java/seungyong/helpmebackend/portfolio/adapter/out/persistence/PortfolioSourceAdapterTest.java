package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import seungyong.helpmebackend.activity.adapter.out.persistence.entity.ActivityJpaEntity;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioSourcePortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class PortfolioSourceAdapterTest {
    @Autowired private PortfolioSourcePortOut sourcePortOut;
    @Autowired private ReflectionPortOut reflectionPortOut;
    @Autowired private TestEntityManager entityManager;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;

    @Test
    @DisplayName("기간 내 saved 회고와 activity만 조회하고 reflection version 변경을 감지")
    void findCandidatesAndVersion() {
        User savedUser = userPortOut.save(user(null, "portfolio-source-token"));
        Project savedProject = projectPortOut.save(project(savedUser.getId()));
        LocalDate date = LocalDate.of(2026, 7, 25);
        Reflection saved = reflectionPortOut.createIfAbsent(Reflection.builder()
                .projectId(savedProject.getId()).kind(ReflectionKind.DAILY).periodStart(date).periodEnd(date)
                .title("저장 회고").content(reflectionDocument()).status(ReflectionStatus.SAVED)
                .sourceQuality(SourceQuality.COMPLETE).sourceSnapshot(ReflectionSourceSnapshot.empty())
                .generationAttempts((short) 0).version(2).build()).reflection();
        reflectionPortOut.createIfAbsent(Reflection.builder()
                .projectId(savedProject.getId()).kind(ReflectionKind.DAILY).periodStart(date.plusDays(1)).periodEnd(date.plusDays(1))
                .title("초안").content(reflectionDocument()).status(ReflectionStatus.DRAFT)
                .sourceQuality(SourceQuality.COMPLETE).sourceSnapshot(ReflectionSourceSnapshot.empty())
                .generationAttempts((short) 0).version(0).build());
        entityManager.persistAndFlush(ActivityJpaEntity.builder()
                .project(entityManager.getEntityManager().getReference(ProjectJpaEntity.class, savedProject.getId()))
                .externalKey("push:abc").activityType(ActivityType.PUSH_COMMIT)
                .branchName("main").commitSha("a32f91d0")
                .title("Webhook 설정").publicUrl("https://github.com/octocat/helpme/commit/a32f91d0")
                .occurredAt(OffsetDateTime.parse("2026-07-25T12:00:00Z")).build());

        var result = sourcePortOut.findCandidates(
                savedProject.getId(), date, date,
                OffsetDateTime.parse("2026-07-24T15:00:00Z"),
                OffsetDateTime.parse("2026-07-25T15:00:00Z")
        );

        assertThat(result.reflections()).hasSize(1);
        assertThat(result.reflections().get(0).id()).isEqualTo(saved.id());
        assertThat(result.activities()).hasSize(1);
        assertThat(sourcePortOut.reflectionVersionsMatch(savedProject.getId(), List.of(
                new PortfolioSourceSnapshot.ReflectionSource(saved.id(), ReflectionKind.DAILY, date, date,
                        "저장 회고", 2, reflectionDocument())
        ))).isTrue();
        assertThat(sourcePortOut.reflectionVersionsMatch(savedProject.getId(), List.of(
                new PortfolioSourceSnapshot.ReflectionSource(saved.id(), ReflectionKind.DAILY, date, date,
                        "저장 회고", 1, reflectionDocument())
        ))).isFalse();
    }

    private ReflectionDocument reflectionDocument() {
        return new ReflectionDocument(1, List.of(new ReflectionDocument.Section(
                "summary", "markdown", "요약", "완료", List.of()
        )));
    }
}
