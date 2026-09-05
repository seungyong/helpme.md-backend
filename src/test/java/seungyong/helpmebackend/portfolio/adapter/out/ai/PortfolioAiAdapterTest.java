package seungyong.helpmebackend.portfolio.adapter.out.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioGenerationException;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PortfolioAiAdapterTest {
    @Mock private ChatModel chatModel;
    private PortfolioAiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PortfolioAiAdapter(chatModel);
        ReflectionTestUtils.setField(adapter, "portfolioCacheKeyPrefix", "portfolio");
    }

    @Test
    @DisplayName("JSON schema 응답을 evidenceRef가 연결된 포트폴리오 문서로 변환")
    void generate_success() {
        mockResponse("""
                {"sections":[{"id":"overview","type":"project_overview","title":"프로젝트 개요",
                "contentMd":"Webhook 복구 흐름을 구현했다.","evidenceRefs":["reflection:401"]}]}
                """);

        var result = adapter.generate(portfolio());

        assertThat(result.content().sections()).hasSize(1);
        assertThat(result.content().sections().get(0).type()).isEqualTo("project_overview");
    }

    @Test
    @DisplayName("snapshot에 없는 evidenceRef를 AI가 반환하면 PORTFOLIO_50001")
    void generate_unknownEvidence() {
        mockResponse("""
                {"sections":[{"id":"overview","type":"project_overview","title":"프로젝트 개요",
                "contentMd":"근거 없는 내용","evidenceRefs":["activity:999"]}]}
                """);

        assertThatThrownBy(() -> adapter.generate(portfolio()))
                .isInstanceOf(PortfolioGenerationException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PORTFOLIO_50001");
    }

    private void mockResponse(String json) {
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        given(response.getResult().getOutput().getText()).willReturn(json);
        given(chatModel.call(any(Prompt.class))).willReturn(response);
    }

    private Portfolio portfolio() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        ReflectionDocument reflection = new ReflectionDocument(1, List.of(
                new ReflectionDocument.Section("summary", "markdown", "요약", "Webhook 복구 구현", List.of())
        ));
        PortfolioSourceSnapshot source = new PortfolioSourceSnapshot(
                List.of(new PortfolioSourceSnapshot.ReflectionSource(
                        401L, ReflectionKind.DAILY, date, date, "Webhook 복구", 2, reflection
                )), List.of(), List.of()
        );
        return Portfolio.builder().id(501L).projectId(101L).requestKey(UUID.randomUUID()).title("포트폴리오")
                .periodStart(date).periodEnd(date).tone(PortfolioTone.CONCISE).status(PortfolioStatus.GENERATING)
                .content(PortfolioDocument.empty()).sourceSnapshot(source).generationAttempts((short) 1)
                .sourceHash("hash").version(0).build();
    }
}
