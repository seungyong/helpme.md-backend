package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioPortOut;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class PortfolioAdapterTest {
    @Autowired private PortfolioPortOut portfolioPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private UserPortOut userPortOut;

    @Test
    @DisplayName("idempotency, claim, 생성, version 저장과 재생성 실패 시 이전 문서 보존을 DB에서 보장")
    void lifecycle() {
        Project savedProject = saveProject("portfolio-token");
        Portfolio queued = portfolio(savedProject.getId(), PortfolioStatus.QUEUED, (short) 0, null);

        var created = portfolioPortOut.createIfAbsent(queued);
        var duplicate = portfolioPortOut.createIfAbsent(queued);
        assertThat(created.created()).isTrue();
        assertThat(duplicate.created()).isFalse();
        assertThat(duplicate.portfolio().id()).isEqualTo(created.portfolio().id());

        Portfolio claimed = portfolioPortOut.claimNext(
                OffsetDateTime.parse("2026-09-05T12:00:00Z"),
                OffsetDateTime.parse("2026-09-05T11:55:00Z")
        ).orElseThrow();
        assertThat(claimed.status()).isEqualTo(PortfolioStatus.GENERATING);
        assertThat(claimed.generationAttempts()).isEqualTo((short) 1);

        PortfolioDocument generated = document("AI 생성 내용");
        portfolioPortOut.completeGeneration(
                claimed.id(), generated, OffsetDateTime.parse("2026-09-05T12:00:10Z")
        );
        Portfolio draft = portfolioPortOut.getByProjectIdAndId(savedProject.getId(), claimed.id()).orElseThrow();
        assertThat(draft.status()).isEqualTo(PortfolioStatus.DRAFT);
        assertThat(draft.version()).isEqualTo(1);

        assertThat(portfolioPortOut.saveIfVersionMatches(
                savedProject.getId(), claimed.id(), "저장", PortfolioTone.REFLECTION,
                document("직접 수정"), 0, OffsetDateTime.parse("2026-09-05T12:01:00Z")
        )).isEmpty();
        Portfolio saved = portfolioPortOut.saveIfVersionMatches(
                savedProject.getId(), claimed.id(), "저장", PortfolioTone.REFLECTION,
                document("직접 수정"), 1, OffsetDateTime.parse("2026-09-05T12:01:00Z")
        ).orElseThrow();
        assertThat(saved.status()).isEqualTo(PortfolioStatus.SAVED);
        assertThat(saved.version()).isEqualTo(2);

        portfolioPortOut.queueRegeneration(savedProject.getId(), saved.id(), saved.sourceSnapshot(), saved.sourceHash());
        portfolioPortOut.claimNext(
                OffsetDateTime.parse("2026-09-05T12:02:00Z"),
                OffsetDateTime.parse("2026-09-05T11:57:00Z")
        ).orElseThrow();
        portfolioPortOut.failGeneration(saved.id(), "PORTFOLIO_50001", "생성 실패");

        Portfolio failed = portfolioPortOut.getByProjectIdAndId(savedProject.getId(), saved.id()).orElseThrow();
        assertThat(failed.status()).isEqualTo(PortfolioStatus.FAILED);
        assertThat(failed.version()).isEqualTo(2);
        assertThat(failed.content().sections().get(0).contentMd()).isEqualTo("직접 수정");
    }

    @Test
    @DisplayName("3회 선점 후 5분 넘게 generating인 포트폴리오는 failed로 복구")
    void stuckAtMaxAttempts_failed() {
        Project savedProject = saveProject("portfolio-stuck-token");
        Portfolio stuck = portfolio(
                savedProject.getId(), PortfolioStatus.GENERATING, (short) 3,
                OffsetDateTime.parse("2026-09-05T10:00:00Z")
        );
        Portfolio created = portfolioPortOut.createIfAbsent(stuck).portfolio();

        assertThat(portfolioPortOut.claimNext(
                OffsetDateTime.parse("2026-09-05T11:00:00Z"),
                OffsetDateTime.parse("2026-09-05T10:55:00Z")
        )).isEmpty();

        Portfolio failed = portfolioPortOut.getByProjectIdAndId(savedProject.getId(), created.id()).orElseThrow();
        assertThat(failed.status()).isEqualTo(PortfolioStatus.FAILED);
        assertThat(failed.error().code()).isEqualTo("PORTFOLIO_50001");
    }

    private Project saveProject(String token) {
        User savedUser = userPortOut.save(user(null, token));
        return projectPortOut.save(project(savedUser.getId()));
    }

    private Portfolio portfolio(Long projectId, PortfolioStatus status, short attempts, OffsetDateTime startedAt) {
        return Portfolio.builder().projectId(projectId).requestKey(UUID.randomUUID()).title("포트폴리오")
                .periodStart(LocalDate.of(2026, 5, 1)).periodEnd(LocalDate.of(2026, 7, 31))
                .tone(PortfolioTone.CONCISE).status(status).content(PortfolioDocument.empty())
                .sourceSnapshot(PortfolioSourceSnapshot.empty()).sourceHash("hash")
                .generationAttempts(attempts).generationStartedAt(startedAt).version(0).build();
    }

    private PortfolioDocument document(String content) {
        return new PortfolioDocument(1, List.of(new PortfolioDocument.Section(
                "overview", "project_overview", "개요", content, List.of()
        )));
    }
}
