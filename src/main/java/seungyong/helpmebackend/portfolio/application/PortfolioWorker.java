package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioGenerationPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.result.GeneratedPortfolio;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioGenerationException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioWorker {
    private static final Duration STUCK_AFTER = Duration.ofMinutes(5);

    private final PortfolioPortOut portfolioPortOut;
    private final PortfolioGenerationPortOut generationPortOut;

    @Scheduled(fixedDelayString = "${workers.portfolio.fixed-delay-ms:1000}")
    public void runOnce() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        portfolioPortOut.claimNext(now, now.minus(STUCK_AFTER)).ifPresent(this::process);
    }

    private void process(Portfolio portfolio) {
        try {
            // 요청 시 고정한 snapshot만 AI에 전달하여 이후 회고 수정이 생성 결과를 바꾸지 않도록 처리
            GeneratedPortfolio generated = generationPortOut.generate(portfolio);
            portfolioPortOut.completeGeneration(
                    portfolio.id(), generated.content(), OffsetDateTime.now(ZoneOffset.UTC)
            );
        } catch (PortfolioGenerationException exception) {
            portfolioPortOut.failGeneration(portfolio.id(), exception.getErrorCode(), exception.getMessage());
            log.warn("Portfolio generation failed: portfolioId={}, code={}", portfolio.id(), exception.getErrorCode());
        } catch (RuntimeException exception) {
            PortfolioErrorCode code = PortfolioErrorCode.PORTFOLIO_GENERATION_FAILED;
            portfolioPortOut.failGeneration(portfolio.id(), code.getErrorCode(), code.getMessage());
            log.warn("Portfolio generation failed: portfolioId={}, exceptionType={}",
                    portfolio.id(), exception.getClass().getSimpleName());
        }
    }
}
