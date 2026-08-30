package seungyong.helpmebackend.reflection.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
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
class ReflectionAdapterTest {
    @Autowired private ReflectionPortOut reflectionPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;

    @Test
    @DisplayName("기간 unique, 작업 claim, 생성 완료, version 조건 저장을 DB에서 보장")
    void lifecycle() {
        User savedUser = userPortOut.save(user(null, "test-token"));
        Project savedProject = projectPortOut.save(project(savedUser.getId()));
        Reflection queued = queued(savedProject.getId(), LocalDate.of(2026, 8, 30));

        ReflectionPortOut.CreateResult created = reflectionPortOut.createIfAbsent(queued);
        ReflectionPortOut.CreateResult duplicate = reflectionPortOut.createIfAbsent(queued);

        assertThat(created.created()).isTrue();
        assertThat(duplicate.created()).isFalse();
        assertThat(duplicate.reflection().id()).isEqualTo(created.reflection().id());

        Reflection claimed = reflectionPortOut.claimNext(
                OffsetDateTime.parse("2026-08-30T12:00:00Z"),
                OffsetDateTime.parse("2026-08-30T11:55:00Z")
        ).orElseThrow();
        assertThat(claimed.status()).isEqualTo(ReflectionStatus.GENERATING);
        assertThat(claimed.generationAttempts()).isEqualTo((short) 1);

        ReflectionDocument generated = document("생성 내용");
        reflectionPortOut.completeGeneration(
                claimed.id(), "생성된 회고", generated,
                SourceQuality.COMPLETE, snapshot(), "generated-hash",
                OffsetDateTime.parse("2026-08-30T12:00:10Z")
        );
        Reflection draft = reflectionPortOut.getByProjectIdAndId(
                savedProject.getId(), claimed.id()
        ).orElseThrow();
        assertThat(draft.status()).isEqualTo(ReflectionStatus.DRAFT);
        assertThat(draft.version()).isEqualTo(1);

        assertThat(reflectionPortOut.saveIfVersionMatches(
                savedProject.getId(), claimed.id(), "저장한 회고",
                document("수정 내용"), 0,
                OffsetDateTime.parse("2026-08-30T12:01:00Z")
        )).isEmpty();
        Reflection saved = reflectionPortOut.saveIfVersionMatches(
                savedProject.getId(), claimed.id(), "저장한 회고",
                document("수정 내용"), 1,
                OffsetDateTime.parse("2026-08-30T12:01:00Z")
        ).orElseThrow();
        assertThat(saved.status()).isEqualTo(ReflectionStatus.SAVED);
        assertThat(saved.version()).isEqualTo(2);
        assertThat(saved.content().summary()).isEqualTo("수정 내용");

        Reflection requeued = reflectionPortOut.queueRegeneration(
                savedProject.getId(), claimed.id()
        ).orElseThrow();
        assertThat(requeued.generationAttempts()).isZero();
        assertThat(requeued.sourceHash()).isEqualTo("generated-hash");
        Reflection reclaimed = reflectionPortOut.claimNext(
                OffsetDateTime.parse("2026-08-30T12:02:00Z"),
                OffsetDateTime.parse("2026-08-30T11:57:00Z")
        ).orElseThrow();
        assertThat(reclaimed.generationAttempts()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("3회 선점 후 5분 넘게 generating인 작업은 failed로 복구")
    void stuckAtMaxAttempts_failed() {
        User savedUser = userPortOut.save(user(null, "stuck-token"));
        Project savedProject = projectPortOut.save(project(savedUser.getId()));
        Reflection stuck = Reflection.builder()
                .projectId(savedProject.getId())
                .kind(ReflectionKind.DAILY)
                .periodStart(LocalDate.of(2026, 8, 29))
                .periodEnd(LocalDate.of(2026, 8, 29))
                .content(ReflectionDocument.empty())
                .status(ReflectionStatus.GENERATING)
                .sourceQuality(SourceQuality.COMPLETE)
                .sourceSnapshot(snapshot())
                .sourceHash("hash")
                .generationAttempts((short) 3)
                .generationStartedAt(OffsetDateTime.parse("2026-08-30T10:00:00Z"))
                .version(0)
                .build();
        Reflection created = reflectionPortOut.createIfAbsent(stuck).reflection();

        assertThat(reflectionPortOut.claimNext(
                OffsetDateTime.parse("2026-08-30T11:00:00Z"),
                OffsetDateTime.parse("2026-08-30T10:55:00Z")
        )).isEmpty();

        Reflection failed = reflectionPortOut.getByProjectIdAndId(
                savedProject.getId(), created.id()
        ).orElseThrow();
        assertThat(failed.status()).isEqualTo(ReflectionStatus.FAILED);
        assertThat(failed.error().code()).isEqualTo("REFLECTION_50001");
    }

    private Reflection queued(Long projectId, LocalDate date) {
        return Reflection.builder()
                .projectId(projectId)
                .kind(ReflectionKind.DAILY)
                .periodStart(date)
                .periodEnd(date)
                .content(ReflectionDocument.empty())
                .status(ReflectionStatus.QUEUED)
                .sourceQuality(SourceQuality.COMPLETE)
                .sourceSnapshot(snapshot())
                .sourceHash("hash")
                .generationAttempts((short) 0)
                .version(0)
                .build();
    }

    private ReflectionSourceSnapshot snapshot() {
        return new ReflectionSourceSnapshot(
                1, 0, List.of(new ReflectionSourceSnapshot.Evidence(
                "activity:801", "feat", "main · abc", "기능 구현"
        )), null, null, List.of(), 0, List.of(), false);
    }

    private ReflectionDocument document(String content) {
        return new ReflectionDocument(1, List.of(
                new ReflectionDocument.Section(
                        "summary", "markdown", "요약", content, List.of("activity:801")
                )
        ));
    }
}
