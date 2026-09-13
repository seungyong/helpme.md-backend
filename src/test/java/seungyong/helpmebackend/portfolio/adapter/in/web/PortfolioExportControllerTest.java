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
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioExportPortIn;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PortfolioExportController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = AuthenticationFilter.class
))
@Import(TestSecurityConfig.class)
class PortfolioExportControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private PortfolioExportPortIn portIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("PDF 시작은 v1 Location과 Retry-After를 포함한 202")
    void startPdf_accepted() throws Exception {
        given(portIn.startExport(any())).willReturn(export(PortfolioExportStatus.QUEUED));

        mockMvc.perform(post("/api/v1/projects/101/portfolios/501/exports").with(user())
                        .header("Idempotency-Key", "2a354265-0000-4000-8000-000000000001")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"format":"pdf","portfolioVersion":3,
                                 "options":{"includeCoverAndToc":true,"includeEvidenceLinks":true,
                                            "includePageNumbers":true,"showGeneratedAt":false}}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/projects/101/portfolios/501/exports/701"))
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.exportId").value(701))
                .andExpect(jsonPath("$.portfolioVersion").value(3));
    }

    @Test
    @DisplayName("비동기 최종 실패는 상세 GET 200의 failed와 error")
    void getFailed_ok() throws Exception {
        PortfolioExport failed = PortfolioExport.builder().id(701L).portfolioId(501L).projectId(101L)
                .format(PortfolioExportFormat.PDF).status(PortfolioExportStatus.FAILED)
                .portfolioVersion(3).document(document())
                .options(PortfolioExportOptions.defaults(PortfolioExportFormat.PDF))
                .attempts((short) 1).errorCode("EXPORT_50001").errorMessage("PDF 생성 실패")
                .createdAt(OffsetDateTime.parse("2026-09-06T00:00:00Z")).build();
        given(portIn.getExport(1L, 101L, 501L, 701L)).willReturn(failed);

        mockMvc.perform(get("/api/v1/projects/101/portfolios/501/exports/701").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error.code").value("EXPORT_50001"));
    }

    @Test
    @DisplayName("지원하지 않는 목록 format은 REQ_400")
    void getExports_invalidFormat() throws Exception {
        mockMvc.perform(get("/api/v1/projects/101/portfolios/501/exports?format=word").with(user()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));
    }

    private PortfolioExport export(PortfolioExportStatus status) {
        return PortfolioExport.builder().id(701L).portfolioId(501L).projectId(101L)
                .format(PortfolioExportFormat.PDF).status(status)
                .idempotencyKey(UUID.fromString("2a354265-0000-4000-8000-000000000001"))
                .portfolioVersion(3).document(document())
                .options(PortfolioExportOptions.defaults(PortfolioExportFormat.PDF))
                .attempts((short) 1).build();
    }

    private PortfolioExportDocument document() {
        return new PortfolioExportDocument(1, "포트폴리오", LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), PortfolioDocument.empty(), List.of());
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(1L, "USER"));
    }
}
