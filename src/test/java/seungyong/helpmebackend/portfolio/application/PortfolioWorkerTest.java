package seungyong.helpmebackend.portfolio.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioGenerationPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.result.GeneratedPortfolio;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioGenerationException;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioWorkerTest {
    @Mock private PortfolioPortOut portfolioPortOut;
    @Mock private PortfolioGenerationPortOut generationPortOut;
    private PortfolioWorker worker;

    @BeforeEach
    void setUp() {
        worker = new PortfolioWorker(portfolioPortOut, generationPortOut);
    }

    @Test
    @DisplayName("claim한 snapshot으로 AI 생성 후 같은 portfolioId를 draft로 완료")
    void generate_success() {
        Portfolio portfolio = portfolio();
        PortfolioDocument document = new PortfolioDocument(1, List.of(
                new PortfolioDocument.Section("overview", "project_overview", "개요", "내용",
                        List.of("reflection:401"))
        ));
        given(portfolioPortOut.claimNext(any(), any())).willReturn(Optional.of(portfolio));
        given(generationPortOut.generate(portfolio)).willReturn(new GeneratedPortfolio(document));

        worker.runOnce();

        verify(portfolioPortOut).completeGeneration(eq(501L), eq(document), any());
        verify(portfolioPortOut, never()).failGeneration(any(), any(), any());
    }

    @Test
    @DisplayName("AI 실패를 HTTP 시작 오류로 잃지 않고 portfolio failed/error에 저장")
    void generate_failed() {
        Portfolio portfolio = portfolio();
        given(portfolioPortOut.claimNext(any(), any())).willReturn(Optional.of(portfolio));
        given(generationPortOut.generate(portfolio)).willThrow(new PortfolioGenerationException(
                "RATE_42902", "요청 한도 초과", new IllegalStateException()
        ));

        worker.runOnce();

        verify(portfolioPortOut).failGeneration(501L, "RATE_42902", "요청 한도 초과");
    }

    private Portfolio portfolio() {
        return Portfolio.builder().id(501L).projectId(101L).requestKey(UUID.randomUUID()).title("포트폴리오")
                .periodStart(LocalDate.of(2026, 5, 1)).periodEnd(LocalDate.of(2026, 7, 31))
                .tone(PortfolioTone.CONCISE).status(PortfolioStatus.GENERATING)
                .content(PortfolioDocument.empty()).sourceSnapshot(PortfolioSourceSnapshot.empty())
                .sourceHash("hash").generationAttempts((short) 1).version(0).build();
    }
}
