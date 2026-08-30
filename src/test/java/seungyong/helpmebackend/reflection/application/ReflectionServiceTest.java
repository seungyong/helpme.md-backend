package seungyong.helpmebackend.reflection.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.application.port.in.command.CreateReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.in.command.RegenerateReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.in.command.SaveReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionErrorCode;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReflectionServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 30);

    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private ReflectionPortOut reflectionPortOut;
    @Mock private ReflectionSourceBuilder sourceBuilder;
    private ReflectionService service;

    @BeforeEach
    void setUp() {
        service = new ReflectionService(
                projectAccessResolver, reflectionPortOut, sourceBuilder
        );
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID))
                .willReturn(Project.builder()
                        .id(PROJECT_ID)
                        .userId(USER_ID)
                        .repoFullName("octocat/helpme")
                        .build());
    }

    @Test
    @DisplayName("AI 회고는 근거 snapshot과 함께 queued로 생성")
    void createAi_queued() {
        given(reflectionPortOut.getByPeriod(
                PROJECT_ID, ReflectionKind.DAILY, DATE
        )).willReturn(Optional.empty());
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.COMPLETE, true));
        given(reflectionPortOut.createIfAbsent(any())).willAnswer(invocation ->
                new ReflectionPortOut.CreateResult(
                        withId(invocation.getArgument(0), 401L), true
                ));

        var result = service.createReflection(create("ai", true));

        assertThat(result.reflectionId()).isEqualTo(401L);
        assertThat(result.status()).isEqualTo(ReflectionStatus.QUEUED);
        assertThat(result.asynchronous()).isTrue();
        assertThat(result.retryAfterSeconds()).isEqualTo(2);
    }

    @Test
    @DisplayName("근거가 없는 AI 생성은 row를 만들지 않고 REFLECTION_42201")
    void createAi_withoutSource() {
        given(reflectionPortOut.getByPeriod(
                PROJECT_ID, ReflectionKind.DAILY, DATE
        )).willReturn(Optional.empty());
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.COMPLETE, false));

        assertThatThrownBy(() -> service.createReflection(create("ai", true)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", ReflectionErrorCode.REFLECTION_SOURCE_INSUFFICIENT
                );
        verify(reflectionPortOut, never()).createIfAbsent(any());
    }

    @Test
    @DisplayName("blank 생성은 근거가 없어도 즉시 draft를 생성")
    void createBlank_withoutSource() {
        given(reflectionPortOut.getByPeriod(
                PROJECT_ID, ReflectionKind.DAILY, DATE
        )).willReturn(Optional.empty());
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.COMPLETE, false));
        given(reflectionPortOut.createIfAbsent(any())).willAnswer(invocation ->
                new ReflectionPortOut.CreateResult(
                        withId(invocation.getArgument(0), 402L), true
                ));

        var result = service.createReflection(create("blank", false));

        assertThat(result.status()).isEqualTo(ReflectionStatus.DRAFT);
        assertThat(result.asynchronous()).isFalse();
    }

    @Test
    @DisplayName("generationMode 생략은 GPT를 호출하지 않는 blank draft로 생성")
    void createWithoutMode_defaultsToBlank() {
        given(reflectionPortOut.getByPeriod(
                PROJECT_ID, ReflectionKind.DAILY, DATE
        )).willReturn(Optional.empty());
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.COMPLETE, false));
        given(reflectionPortOut.createIfAbsent(any())).willAnswer(invocation ->
                new ReflectionPortOut.CreateResult(
                        withId(invocation.getArgument(0), 403L), true
                ));

        var result = service.createReflection(create(null, false));

        assertThat(result.status()).isEqualTo(ReflectionStatus.DRAFT);
        assertThat(result.asynchronous()).isFalse();
        assertThat(result.retryAfterSeconds()).isZero();
    }

    @Test
    @DisplayName("같은 기간 중복 생성은 기존 회고를 반환하고 근거를 다시 조회하지 않음")
    void createDuplicate_returnsExisting() {
        Reflection existing = reflection(401L, ReflectionStatus.SAVED, 3);
        given(reflectionPortOut.getByPeriod(
                PROJECT_ID, ReflectionKind.DAILY, DATE
        )).willReturn(Optional.of(existing));

        var result = service.createReflection(create("ai", true));

        assertThat(result.reflectionId()).isEqualTo(401L);
        assertThat(result.created()).isFalse();
        verify(sourceBuilder, never()).build(any(), any(), any(), any());
    }

    @Test
    @DisplayName("저장 요청 version이 최신 회고와 다르면 DOCUMENT_40901")
    void save_versionConflict() {
        given(reflectionPortOut.getByProjectIdAndId(PROJECT_ID, 401L))
                .willReturn(Optional.of(reflection(401L, ReflectionStatus.DRAFT, 4)));

        assertThatThrownBy(() -> service.saveReflection(new SaveReflectionCommand(
                USER_ID, PROJECT_ID, 401L, "수정", document(), 3
        ))).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", DocumentErrorCode.DOCUMENT_VERSION_CONFLICT
                );
        verify(reflectionPortOut, never()).saveIfVersionMatches(
                any(), any(), any(), any(), any(Integer.class), any()
        );
    }

    @Test
    @DisplayName("allowPartial=false 재생성은 partial 근거를 거부")
    void regenerate_partialNotAllowed() {
        Reflection current = reflection(401L, ReflectionStatus.SAVED, 2);
        given(reflectionPortOut.getByProjectIdAndId(PROJECT_ID, 401L))
                .willReturn(Optional.of(current));
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.PARTIAL, true));

        assertThatThrownBy(() -> service.regenerateReflection(
                new RegenerateReflectionCommand(USER_ID, PROJECT_ID, 401L, false)
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", ReflectionErrorCode.REFLECTION_SOURCE_INSUFFICIENT
                );
        verify(reflectionPortOut, never()).queueRegeneration(
                any(), any()
        );
    }

    @Test
    @DisplayName("마지막 AI 성공 sourceHash와 최신 hash가 같으면 재생성을 queue하지 않음")
    void regenerate_sameSuccessfullyGeneratedSource_returnsExisting() {
        Reflection current = reflection(401L, ReflectionStatus.SAVED, 2);
        given(reflectionPortOut.getByProjectIdAndId(PROJECT_ID, 401L))
                .willReturn(Optional.of(current));
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.COMPLETE, true));

        var result = service.regenerateReflection(
                new RegenerateReflectionCommand(USER_ID, PROJECT_ID, 401L, true)
        );

        assertThat(result.reflectionId()).isEqualTo(401L);
        assertThat(result.status()).isEqualTo(ReflectionStatus.SAVED);
        assertThat(result.asynchronous()).isFalse();
        assertThat(result.retryAfterSeconds()).isZero();
        verify(reflectionPortOut, never()).queueRegeneration(any(), any());
    }

    @Test
    @DisplayName("동일 sourceHash라도 이전 생성이 failed면 재시도를 queue")
    void regenerate_sameSourceAfterFailure_queuesRetry() {
        Reflection current = reflection(401L, ReflectionStatus.FAILED, 2);
        Reflection queued = reflection(401L, ReflectionStatus.QUEUED, 2);
        given(reflectionPortOut.getByProjectIdAndId(PROJECT_ID, 401L))
                .willReturn(Optional.of(current));
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.COMPLETE, true));
        given(reflectionPortOut.queueRegeneration(PROJECT_ID, 401L))
                .willReturn(Optional.of(queued));

        var result = service.regenerateReflection(
                new RegenerateReflectionCommand(USER_ID, PROJECT_ID, 401L, true)
        );

        assertThat(result.status()).isEqualTo(ReflectionStatus.QUEUED);
        assertThat(result.asynchronous()).isTrue();
        verify(reflectionPortOut).queueRegeneration(PROJECT_ID, 401L);
    }

    @Test
    @DisplayName("마지막 AI 성공 이후 sourceHash가 바뀌면 재생성을 queue")
    void regenerate_changedSource_queues() {
        Reflection current = reflection(
                401L, ReflectionStatus.SAVED, 2,
                "old-hash", OffsetDateTime.of(
                        2026, 8, 30, 12, 0, 0, 0, ZoneOffset.UTC
                )
        );
        Reflection queued = reflection(401L, ReflectionStatus.QUEUED, 2);
        given(reflectionPortOut.getByProjectIdAndId(PROJECT_ID, 401L))
                .willReturn(Optional.of(current));
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willReturn(source(SourceQuality.COMPLETE, true));
        given(reflectionPortOut.queueRegeneration(PROJECT_ID, 401L))
                .willReturn(Optional.of(queued));

        var result = service.regenerateReflection(
                new RegenerateReflectionCommand(USER_ID, PROJECT_ID, 401L, true)
        );

        assertThat(result.status()).isEqualTo(ReflectionStatus.QUEUED);
        assertThat(result.asynchronous()).isTrue();
        verify(reflectionPortOut).queueRegeneration(PROJECT_ID, 401L);
    }

    private CreateReflectionCommand create(String mode, boolean allowPartial) {
        return new CreateReflectionCommand(
                USER_ID, PROJECT_ID, "daily", DATE, mode, allowPartial
        );
    }

    private ReflectionSourceBuilder.Result source(
            SourceQuality quality, boolean exists
    ) {
        ReflectionSourceSnapshot snapshot = new ReflectionSourceSnapshot(
                exists ? 1 : 0,
                0,
                exists ? List.of(new ReflectionSourceSnapshot.Evidence(
                        "activity:1", "feat", "main · abc", "기능 구현"
                )) : List.of(),
                null, null, List.of(), 0, List.of(), quality == SourceQuality.PARTIAL
        );
        return new ReflectionSourceBuilder.Result(snapshot, quality, "hash");
    }

    private Reflection reflection(Long id, ReflectionStatus status, int version) {
        return reflection(
                id, status, version, "hash",
                OffsetDateTime.of(2026, 8, 30, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    private Reflection reflection(
            Long id,
            ReflectionStatus status,
            int version,
            String sourceHash,
            OffsetDateTime generatedAt
    ) {
        return Reflection.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .kind(ReflectionKind.DAILY)
                .periodStart(DATE)
                .periodEnd(DATE)
                .title("회고")
                .content(document())
                .status(status)
                .sourceQuality(SourceQuality.COMPLETE)
                .sourceSnapshot(source(SourceQuality.COMPLETE, true).snapshot())
                .sourceHash(sourceHash)
                .generationAttempts((short) 0)
                .generatedAt(generatedAt)
                .version(version)
                .build();
    }

    private Reflection withId(Reflection reflection, Long id) {
        return Reflection.builder()
                .id(id)
                .projectId(reflection.projectId())
                .kind(reflection.kind())
                .periodStart(reflection.periodStart())
                .periodEnd(reflection.periodEnd())
                .title(reflection.title())
                .content(reflection.content())
                .status(reflection.status())
                .sourceQuality(reflection.sourceQuality())
                .sourceSnapshot(reflection.sourceSnapshot())
                .sourceHash(reflection.sourceHash())
                .generationAttempts(reflection.generationAttempts())
                .version(reflection.version())
                .build();
    }

    private ReflectionDocument document() {
        return new ReflectionDocument(1, List.of(
                new ReflectionDocument.Section(
                        "summary", "markdown", "요약", "내용", List.of("activity:1")
                )
        ));
    }
}
