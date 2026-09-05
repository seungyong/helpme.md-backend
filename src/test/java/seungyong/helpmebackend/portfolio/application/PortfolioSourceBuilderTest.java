package seungyong.helpmebackend.portfolio.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.portfolio.application.port.in.command.CustomEvidenceLinkCommand;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioSourcePortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceData;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PortfolioSourceBuilderTest {
    @Mock private PortfolioSourcePortOut sourcePortOut;
    private PortfolioSourceBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new PortfolioSourceBuilder(sourcePortOut);
    }

    @Test
    @DisplayName("선택한 saved 회고의 version과 본문을 고정 snapshot/hash로 생성")
    void build_snapshot() {
        given(sourcePortOut.findSelected(101L, List.of(401L), List.of())).willReturn(
                new PortfolioSourceData(List.of(reflection()), List.of())
        );

        PortfolioSourceBuildResult result = builder.build(
                project(false), List.of(401L), List.of(), List.of(
                        new CustomEvidenceLinkCommand("공개 데모", "https://example.com/demo")
                )
        );

        assertThat(result.snapshot().reflections().get(0).version()).isEqualTo(2);
        assertThat(result.snapshot().reflections().get(0).content().summary()).isEqualTo("구현 완료");
        assertThat(result.sourceHash()).hasSize(64);
    }

    @Test
    @DisplayName("private Repository activity를 직접 선택하면 PORTFOLIO_42202")
    void build_privateActivity() {
        given(sourcePortOut.findSelected(101L, List.of(401L), List.of(801L))).willReturn(
                new PortfolioSourceData(List.of(reflection()), List.of(new PortfolioSourceData.ActivityData(
                        801L, seungyong.helpmebackend.activity.domain.type.ActivityType.PUSH_COMMIT,
                        "main", "a32f91d0", "구현", "https://github.com/octocat/helpme/commit/a32f91d0"
                )))
        );

        assertThatThrownBy(() -> builder.build(
                project(true), List.of(401L), List.of(801L), List.of()
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("서명 query가 포함된 custom link는 공개 근거로 저장하지 않음")
    void build_signedLink() {
        given(sourcePortOut.findSelected(101L, List.of(401L), List.of())).willReturn(
                new PortfolioSourceData(List.of(reflection()), List.of())
        );

        assertThatThrownBy(() -> builder.build(
                project(false), List.of(401L), List.of(), List.of(
                        new CustomEvidenceLinkCommand("임시 링크", "https://example.com/file?token=secret")
                )
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
    }

    private Project project(boolean privateRepository) {
        return Project.builder().id(101L).userId(1L).repoFullName("octocat/helpme")
                .privateRepository(privateRepository).build();
    }

    private PortfolioSourceData.ReflectionData reflection() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        return new PortfolioSourceData.ReflectionData(
                401L, ReflectionKind.DAILY, date, date, "회고", 2,
                new ReflectionDocument(1, List.of(new ReflectionDocument.Section(
                        "summary", "markdown", "요약", "구현 완료", List.of()
                )))
        );
    }
}
