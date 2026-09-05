package seungyong.helpmebackend.portfolio.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioPortIn;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioGenerationResult;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioEligibility;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioPage;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PortfolioController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = AuthenticationFilter.class
))
@Import(TestSecurityConfig.class)
class PortfolioControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private PortfolioPortIn portfolioPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("AI 생성은 v1 Location과 Retry-After를 포함한 202")
    void createAi_accepted() throws Exception {
        given(portfolioPortIn.createPortfolio(any())).willReturn(new PortfolioGenerationResult(
                501L, PortfolioStatus.QUEUED, 0, true, true, 2
        ));

        mockMvc.perform(post("/api/v1/projects/101/portfolios").with(user())
                        .header("Idempotency-Key", "9dd7c84d-0000-4000-8000-000000000001")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("ai")))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/projects/101/portfolios/501"))
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.portfolioId").value(501))
                .andExpect(jsonPath("$.status").value("queued"));
    }

    @Test
    @DisplayName("blank 생성은 AI 없이 201 draft")
    void createBlank_created() throws Exception {
        given(portfolioPortIn.createPortfolio(any())).willReturn(new PortfolioGenerationResult(
                501L, PortfolioStatus.DRAFT, 0, true, false, 0
        ));

        mockMvc.perform(post("/api/v1/projects/101/portfolios").with(user())
                        .header("Idempotency-Key", "9dd7c84d-0000-4000-8000-000000000001")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("blank")))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.status").value("draft"));
    }

    @Test
    @DisplayName("비동기 최종 실패는 상세 GET 200의 failed/error")
    void getFailed_ok() throws Exception {
        given(portfolioPortIn.getPortfolio(1L, 101L, 501L)).willReturn(portfolioFailed());

        mockMvc.perform(get("/api/v1/projects/101/portfolios/501").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("PORTFOLIO_50001"));
    }

    @Test
    @DisplayName("지원하지 않는 문서 schemaVersion은 REQ_400")
    void save_invalidSchema() throws Exception {
        mockMvc.perform(put("/api/v1/projects/101/portfolios/501").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"포트폴리오","tone":"concise",
                                 "content":{"schemaVersion":2,"sections":[]},"version":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));
    }

    @Test
    @DisplayName("재생성 body 생략은 기존 snapshot으로 202")
    void regenerate_withoutBody() throws Exception {
        given(portfolioPortIn.regeneratePortfolio(any())).willReturn(new PortfolioGenerationResult(
                501L, PortfolioStatus.QUEUED, 2, false, true, 2
        ));

        mockMvc.perform(post("/api/v1/projects/101/portfolios/501/regenerate").with(user()))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Retry-After", "2"));
    }

    @Test
    @DisplayName("포트폴리오가 없고 saved 회고도 없으면 오류가 아닌 empty/eligibility 200")
    void list_emptyAndIneligible() throws Exception {
        given(portfolioPortIn.getPortfolios(any())).willReturn(new PortfolioPage(
                List.of(), PortfolioEligibility.from(0), null, false
        ));

        mockMvc.perform(get("/api/v1/projects/101/portfolios").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.eligibility.canCreate").value(false))
                .andExpect(jsonPath("$.eligibility.reason").value("saved_reflection_required"))
                .andExpect(jsonPath("$.page.hasNext").value(false));
    }

    private String createBody(String mode) {
        return """
                {"title":"포트폴리오","periodStart":"2026-05-01","periodEnd":"2026-07-31",
                 "tone":"concise","reflectionIds":[401],"activityIds":[],
                 "customEvidenceLinks":[],"generationMode":"%s"}
                """.formatted(mode);
    }

    private Portfolio portfolioFailed() {
        return Portfolio.builder().id(501L).projectId(101L).requestKey(UUID.randomUUID()).title("포트폴리오")
                .periodStart(LocalDate.of(2026, 5, 1)).periodEnd(LocalDate.of(2026, 7, 31))
                .tone(PortfolioTone.CONCISE).status(PortfolioStatus.FAILED)
                .content(PortfolioDocument.empty()).sourceSnapshot(PortfolioSourceSnapshot.empty())
                .sourceHash("hash").generationAttempts((short) 1)
                .error(new Portfolio.PortfolioError("PORTFOLIO_50001", "생성 실패", true))
                .version(0).build();
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(1L, "seungyong"));
    }
}
