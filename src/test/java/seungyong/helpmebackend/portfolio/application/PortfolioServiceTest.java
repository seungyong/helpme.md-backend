package seungyong.helpmebackend.portfolio.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.portfolio.application.port.in.command.CreatePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.in.command.GetPortfolioSourcesQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.ListPortfoliosQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.RegeneratePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.in.command.SavePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioSourcePortOut;
import seungyong.helpmebackend.portfolio.application.port.out.result.PortfolioCreateResult;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceData;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    private static final long USER_ID = 1L;
    private static final long PROJECT_ID = 101L;
    private static final UUID KEY = UUID.fromString("9dd7c84d-0000-4000-8000-000000000001");
    private static final LocalDate START = LocalDate.of(2026, 5, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 31);

    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private PortfolioPortOut portfolioPortOut;
    @Mock private PortfolioSourcePortOut sourcePortOut;
    @Mock private PortfolioSourceBuilder sourceBuilder;
    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioService(projectAccessResolver, portfolioPortOut, sourcePortOut, sourceBuilder);
    }

    @Test
    @DisplayName("private 프로젝트의 activity URL을 근거 조회 응답에서 숨기고 선택 불가 처리")
    void getSources_privateActivity() {
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(true));
        given(sourcePortOut.findCandidates(any(), any(), any(), any(), any())).willReturn(new PortfolioSourceData(
                List.of(reflectionData()), List.of(activityData())
        ));

        var result = service.getSources(new GetPortfolioSourcesQuery(USER_ID, PROJECT_ID, START, END));

        assertThat(result.eligibility().canCreate()).isTrue();
        assertThat(result.evidenceCandidates().get(0).publicUrl()).isNull();
        assertThat(result.evidenceCandidates().get(0).selectable()).isFalse();
        assertThat(result.evidenceCandidates().get(0).unavailableReason()).isEqualTo("private_repository");
    }

    @Test
    @DisplayName("같은 Idempotency-Key 재요청은 근거를 재조회하지 않고 같은 portfolio 반환")
    void create_sameKey_returnsExisting() {
        Portfolio existing = portfolio(501L, PortfolioStatus.QUEUED, 0);
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(false));
        given(portfolioPortOut.getByProjectIdAndRequestKey(PROJECT_ID, KEY)).willReturn(Optional.of(existing));

        var result = service.createPortfolio(createCommand(List.of(401L), "ai"));

        assertThat(result.portfolioId()).isEqualTo(501L);
        assertThat(result.created()).isFalse();
        assertThat(result.asynchronous()).isTrue();
        verify(sourceBuilder, never()).build(any(), any(), any(), any());
    }

    @Test
    @DisplayName("saved 회고 선택이 없으면 PORTFOLIO_42201")
    void create_withoutReflection() {
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(false));
        given(sourceBuilder.build(any(), any(), any(), any()))
                .willThrow(new CustomException(PortfolioErrorCode.PORTFOLIO_SOURCE_REQUIRED));

        assertThatThrownBy(() -> service.createPortfolio(createCommand(List.of(), "ai")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PORTFOLIO_SOURCE_REQUIRED);
    }

    @Test
    @DisplayName("blank 생성은 같은 snapshot을 저장하고 즉시 draft 201 결과")
    void create_blank() {
        PortfolioSourceBuildResult source = new PortfolioSourceBuildResult(snapshot(), "hash");
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(false));
        given(sourceBuilder.build(any(), any(), any(), any())).willReturn(source);
        given(portfolioPortOut.createIfAbsent(any())).willAnswer(invocation ->
                new PortfolioCreateResult(withId(invocation.getArgument(0), 501L), true));

        var result = service.createPortfolio(createCommand(List.of(401L), "blank"));

        assertThat(result.status()).isEqualTo(PortfolioStatus.DRAFT);
        assertThat(result.asynchronous()).isFalse();
    }

    @Test
    @DisplayName("저장 version이 현재 문서와 다르면 DOCUMENT_40901")
    void save_versionConflict() {
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(false));
        given(portfolioPortOut.getByProjectIdAndId(PROJECT_ID, 501L))
                .willReturn(Optional.of(portfolio(501L, PortfolioStatus.DRAFT, 2)));

        assertThatThrownBy(() -> service.savePortfolio(new SavePortfolioCommand(
                USER_ID, PROJECT_ID, 501L, "제목", "concise", PortfolioDocument.empty(), 1
        ))).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
    }

    @Test
    @DisplayName("목록은 size+1 커서 조회 후 eligibility와 다음 커서를 구성")
    void list_withNextCursor() {
        Portfolio first = withUpdatedAt(portfolio(502L, PortfolioStatus.DRAFT, 1), 12);
        Portfolio second = withUpdatedAt(portfolio(501L, PortfolioStatus.SAVED, 2), 11);
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(false));
        given(portfolioPortOut.findPage(PROJECT_ID, null, null, null, 2))
                .willReturn(List.of(first, second));
        given(portfolioPortOut.findLatestExportSummaries(List.of(502L))).willReturn(Map.of());
        given(sourcePortOut.countSavedReflections(PROJECT_ID)).willReturn(3L);

        var result = service.getPortfolios(new ListPortfoliosQuery(
                USER_ID, PROJECT_ID, null, null, 1
        ));

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotBlank();
        assertThat(result.eligibility().currentSavedReflectionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("상세 조회는 snapshot 회고 version이 달라졌으면 sourceChanged=true")
    void get_sourceChanged() {
        Portfolio current = portfolio(501L, PortfolioStatus.SAVED, 2);
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(false));
        given(portfolioPortOut.getByProjectIdAndId(PROJECT_ID, 501L)).willReturn(Optional.of(current));
        given(sourcePortOut.reflectionVersionsMatch(PROJECT_ID, current.sourceSnapshot().reflections()))
                .willReturn(false);

        Portfolio result = service.getPortfolio(USER_ID, PROJECT_ID, 501L);

        assertThat(result.sourceChanged()).isTrue();
    }

    @Test
    @DisplayName("refreshSources=false 재생성은 기존 snapshot/hash를 그대로 queue")
    void regenerate_keepsSnapshot() {
        Portfolio current = portfolio(501L, PortfolioStatus.SAVED, 2);
        Portfolio queued = Portfolio.builder().id(501L).projectId(PROJECT_ID).requestKey(KEY)
                .title(current.title()).periodStart(START).periodEnd(END).tone(PortfolioTone.CONCISE)
                .status(PortfolioStatus.QUEUED).content(current.content())
                .sourceSnapshot(current.sourceSnapshot()).sourceHash(current.sourceHash())
                .generationAttempts((short) 0).version(2).build();
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID)).willReturn(project(false));
        given(portfolioPortOut.getByProjectIdAndId(PROJECT_ID, 501L)).willReturn(Optional.of(current));
        given(portfolioPortOut.queueRegeneration(
                PROJECT_ID, 501L, current.sourceSnapshot(), current.sourceHash()
        )).willReturn(Optional.of(queued));

        var result = service.regeneratePortfolio(new RegeneratePortfolioCommand(
                USER_ID, PROJECT_ID, 501L, false
        ));

        assertThat(result.status()).isEqualTo(PortfolioStatus.QUEUED);
        verify(sourceBuilder, never()).refresh(any(), any());
    }

    private CreatePortfolioCommand createCommand(List<Long> reflections, String mode) {
        return new CreatePortfolioCommand(USER_ID, PROJECT_ID, KEY, "포트폴리오", START, END,
                "concise", reflections, List.of(), List.of(), mode);
    }

    private Project project(boolean privateRepository) {
        return Project.builder().id(PROJECT_ID).userId(USER_ID).repoFullName("octocat/helpme")
                .privateRepository(privateRepository).build();
    }

    private PortfolioSourceData.ReflectionData reflectionData() {
        return new PortfolioSourceData.ReflectionData(401L, ReflectionKind.DAILY, START, START,
                "회고", 2, reflectionDocument());
    }

    private PortfolioSourceData.ActivityData activityData() {
        return new PortfolioSourceData.ActivityData(801L,
                seungyong.helpmebackend.activity.domain.type.ActivityType.PUSH_COMMIT,
                "main", "a32f91d0", "Webhook 설정 UI", "https://github.com/octocat/helpme/commit/a32f91d0");
    }

    private PortfolioSourceSnapshot snapshot() {
        return new PortfolioSourceSnapshot(List.of(new PortfolioSourceSnapshot.ReflectionSource(
                401L, ReflectionKind.DAILY, START, START, "회고", 2, reflectionDocument()
        )), List.of(), List.of());
    }

    private ReflectionDocument reflectionDocument() {
        return new ReflectionDocument(1, List.of(new ReflectionDocument.Section(
                "summary", "markdown", "요약", "구현 완료", List.of()
        )));
    }

    private Portfolio portfolio(Long id, PortfolioStatus status, int version) {
        return Portfolio.builder().id(id).projectId(PROJECT_ID).requestKey(KEY).title("포트폴리오")
                .periodStart(START).periodEnd(END).tone(PortfolioTone.CONCISE).status(status)
                .content(PortfolioDocument.empty()).sourceSnapshot(snapshot()).sourceHash("hash")
                .generationAttempts((short) 0).version(version).build();
    }

    private Portfolio withId(Portfolio source, Long id) {
        return Portfolio.builder().id(id).projectId(source.projectId()).requestKey(source.requestKey())
                .title(source.title()).periodStart(source.periodStart()).periodEnd(source.periodEnd())
                .tone(source.tone()).status(source.status()).content(source.content())
                .sourceSnapshot(source.sourceSnapshot()).sourceHash(source.sourceHash())
                .generationAttempts(source.generationAttempts()).version(source.version()).build();
    }

    private Portfolio withUpdatedAt(Portfolio source, int hour) {
        return Portfolio.builder().id(source.id()).projectId(source.projectId()).requestKey(source.requestKey())
                .title(source.title()).periodStart(source.periodStart()).periodEnd(source.periodEnd())
                .tone(source.tone()).status(source.status()).content(source.content())
                .sourceSnapshot(source.sourceSnapshot()).sourceHash(source.sourceHash())
                .generationAttempts(source.generationAttempts()).version(source.version())
                .updatedAt(OffsetDateTime.of(2026, 9, 5, hour, 0, 0, 0, ZoneOffset.UTC)).build();
    }
}
