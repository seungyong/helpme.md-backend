package seungyong.helpmebackend.reflection.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionGenerationPortOut;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionGenerationException;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReflectionWorkerTest {
    @Mock private ReflectionPortOut reflectionPortOut;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private ReflectionGenerationPortOut generationPortOut;
    @Mock private ReflectionSourceBuilder sourceBuilder;
    private ReflectionWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ReflectionWorker(
                reflectionPortOut, projectPortOut, generationPortOut, sourceBuilder
        );
    }

    @Test
    @DisplayName("claim한 회고를 AI로 생성하고 같은 reflectionId에 draft로 완료")
    void generate_success() {
        Reflection reflection = generating();
        Project project = Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/helpme").build();
        ReflectionDocument document = new ReflectionDocument(1, List.of(
                new ReflectionDocument.Section(
                        "summary", "markdown", "요약", "완료", List.of("activity:801")
                )
        ));
        given(reflectionPortOut.claimNext(any(), any()))
                .willReturn(Optional.of(reflection));
        given(projectPortOut.getById(101L)).willReturn(Optional.of(project));
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source());
        given(generationPortOut.generate(
                eq(project), eq(ReflectionKind.DAILY), any(), any(), any()
        )).willReturn(new ReflectionGenerationPortOut.GeneratedReflection(
                "8월 30일 회고", document
        ));

        worker.runOnce();

        verify(reflectionPortOut).completeGeneration(
                eq(401L), eq("8월 30일 회고"), eq(document),
                eq(SourceQuality.COMPLETE), any(), eq("new-hash"), any()
        );
        verify(reflectionPortOut, never()).failGeneration(any(), any(), any());
    }

    @Test
    @DisplayName("AI rate limit은 worker 예외로 유실하지 않고 failed/error에 저장")
    void generate_rateLimited() {
        Reflection reflection = generating();
        Project project = Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/helpme").build();
        given(reflectionPortOut.claimNext(any(), any()))
                .willReturn(Optional.of(reflection));
        given(projectPortOut.getById(101L)).willReturn(Optional.of(project));
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source());
        given(generationPortOut.generate(any(), any(), any(), any(), any()))
                .willThrow(new ReflectionGenerationException(
                        "RATE_42902", "회고 생성 요청 한도를 초과했습니다.", true,
                        new IllegalStateException()
                ));

        worker.runOnce();

        verify(reflectionPortOut).failGeneration(
                401L, "RATE_42902", "회고 생성 요청 한도를 초과했습니다."
        );
        verify(reflectionPortOut, never()).completeGeneration(
                any(), any(), any(), any(), any(), any(), any()
        );
    }

    private Reflection generating() {
        LocalDate date = LocalDate.of(2026, 8, 30);
        return Reflection.builder()
                .id(401L)
                .projectId(101L)
                .kind(ReflectionKind.DAILY)
                .periodStart(date)
                .periodEnd(date)
                .content(ReflectionDocument.empty())
                .status(ReflectionStatus.GENERATING)
                .sourceQuality(SourceQuality.COMPLETE)
                .sourceSnapshot(new ReflectionSourceSnapshot(
                        1, 0, List.of(new ReflectionSourceSnapshot.Evidence(
                        "activity:801", "feat", "main · abc", "완료"
                )), null, null, List.of(), 0, List.of(), false))
                .generationAttempts((short) 1)
                .version(0)
                .build();
    }

    private ReflectionSourceBuilder.Result source() {
        return new ReflectionSourceBuilder.Result(
                generating().sourceSnapshot(), SourceQuality.COMPLETE, "new-hash"
        );
    }
}
