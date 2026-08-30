package seungyong.helpmebackend.reflection.adapter.in.web;

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
import seungyong.helpmebackend.reflection.application.port.in.ReflectionPortIn;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionGenerationResult;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ReflectionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class ReflectionControllerTest {
    private static final Long PROJECT_ID = 101L;
    private static final Long REFLECTION_ID = 401L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ReflectionPortIn reflectionPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("AI 생성은 /api/v1 Location과 Retry-After를 포함한 202")
    void createAi_accepted() throws Exception {
        given(reflectionPortIn.createReflection(any()))
                .willReturn(new ReflectionGenerationResult(
                        REFLECTION_ID, ReflectionStatus.QUEUED, true, true, 2
                ));

        mockMvc.perform(post("/api/v1/projects/{projectId}/reflections", PROJECT_ID)
                        .with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind":"daily",
                                  "periodStart":"2026-08-30",
                                  "generationMode":"ai",
                                  "allowPartial":true
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/projects/101/reflections/401"
                ))
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.reflectionId").value(401))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.location").value(
                        "/api/v1/projects/101/reflections/401"
                ));
    }

    @Test
    @DisplayName("generationMode 생략은 기본 blank로 즉시 draft 201")
    void createWithoutMode_createdAsBlank() throws Exception {
        given(reflectionPortIn.createReflection(any()))
                .willReturn(new ReflectionGenerationResult(
                        REFLECTION_ID, ReflectionStatus.DRAFT, true, false, 0
                ));

        mockMvc.perform(post("/api/v1/projects/{projectId}/reflections", PROJECT_ID)
                        .with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kind":"daily",
                                  "periodStart":"2026-08-30"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.status").value("draft"));
    }

    @Test
    @DisplayName("비동기 최종 실패는 상세 조회 200의 failed/error")
    void getFailed_ok() throws Exception {
        given(reflectionPortIn.getReflection(1L, PROJECT_ID, REFLECTION_ID))
                .willReturn(reflection(
                        ReflectionKind.DAILY,
                        ReflectionStatus.FAILED,
                        SourceQuality.COMPLETE,
                        new Reflection.ReflectionError(
                                "REFLECTION_50001", "회고 생성에 실패했습니다.", true
                        )
                ));

        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/reflections/{reflectionId}",
                        PROJECT_ID, REFLECTION_ID
                ).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error.code").value("REFLECTION_50001"))
                .andExpect(jsonPath("$.error.retryable").value(true));
    }

    @Test
    @DisplayName("주간 partial 상세는 누락 날짜와 fallback 근거를 반환")
    void getWeeklyPartial_ok() throws Exception {
        given(reflectionPortIn.getReflection(1L, PROJECT_ID, REFLECTION_ID))
                .willReturn(reflection(
                        ReflectionKind.WEEKLY,
                        ReflectionStatus.DRAFT,
                        SourceQuality.PARTIAL,
                        null
                ));

        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/reflections/{reflectionId}",
                        PROJECT_ID, REFLECTION_ID
                ).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceQuality").value("partial"))
                .andExpect(jsonPath("$.sourceSummary.expectedDailyCount").value(7))
                .andExpect(jsonPath("$.sourceSummary.savedDailyCount").value(1))
                .andExpect(jsonPath("$.sourceSummary.missingDailyDates[0]")
                        .value("2026-08-25"))
                .andExpect(jsonPath("$.sourceSummary.fallbackActivityCount").value(1))
                .andExpect(jsonPath("$.sourceSummary.dailyReflections[1].reason")
                        .value("fallback_activity"));
    }

    @Test
    @DisplayName("지원하지 않는 문서 schemaVersion은 REQ_400")
    void save_invalidSchema() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/projects/{projectId}/reflections/{reflectionId}",
                        PROJECT_ID, REFLECTION_ID
                ).with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"회고",
                                  "content":{"schemaVersion":2,"sections":[]},
                                  "version":0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));
    }

    @Test
    @DisplayName("재생성 body가 없어도 allowPartial 기본값으로 202")
    void regenerate_withoutBody() throws Exception {
        given(reflectionPortIn.regenerateReflection(any()))
                .willReturn(new ReflectionGenerationResult(
                        REFLECTION_ID, ReflectionStatus.QUEUED, false, true, 2
                ));

        mockMvc.perform(post(
                        "/api/v1/projects/{projectId}/reflections/{reflectionId}/regenerate",
                        PROJECT_ID, REFLECTION_ID
                ).with(user()))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Retry-After", "2"));
    }

    @Test
    @DisplayName("동일한 성공 sourceHash 재생성은 AI 작업 없이 기존 회고 200")
    void regenerate_sameSource_ok() throws Exception {
        given(reflectionPortIn.regenerateReflection(any()))
                .willReturn(new ReflectionGenerationResult(
                        REFLECTION_ID, ReflectionStatus.SAVED, false, false, 0
                ));

        mockMvc.perform(post(
                        "/api/v1/projects/{projectId}/reflections/{reflectionId}/regenerate",
                        PROJECT_ID, REFLECTION_ID
                ).with(user()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.reflectionId").value(REFLECTION_ID))
                .andExpect(jsonPath("$.status").value("saved"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(0));
    }

    private Reflection reflection(
            ReflectionKind kind,
            ReflectionStatus status,
            SourceQuality quality,
            Reflection.ReflectionError error
    ) {
        LocalDate start = LocalDate.of(2026, 8, 24);
        ReflectionSourceSnapshot source = kind == ReflectionKind.DAILY
                ? new ReflectionSourceSnapshot(
                1, 1, List.of(new ReflectionSourceSnapshot.Evidence(
                "activity:801", "feat", "main · abc", "내용"
        )), null, null, List.of(), 0, List.of(), false)
                : new ReflectionSourceSnapshot(
                2, 1, List.of(), 7, 1,
                List.of(
                        start.plusDays(1), start.plusDays(2), start.plusDays(3),
                        start.plusDays(4), start.plusDays(5), start.plusDays(6)
                ),
                1,
                List.of(
                        new ReflectionSourceSnapshot.DailyReflectionSource(
                                400L, start, "저장 회고", "saved",
                                true, "saved_reflection", "내용"
                        ),
                        new ReflectionSourceSnapshot.DailyReflectionSource(
                                null, start.plusDays(1), null, "missing",
                                false, "fallback_activity", null
                        )
                ),
                false
        );
        return Reflection.builder()
                .id(REFLECTION_ID)
                .projectId(PROJECT_ID)
                .kind(kind)
                .periodStart(start)
                .periodEnd(kind == ReflectionKind.DAILY ? start : start.plusDays(6))
                .title("회고")
                .content(ReflectionDocument.empty())
                .status(status)
                .sourceQuality(quality)
                .sourceSnapshot(source)
                .generationAttempts((short) 1)
                .error(error)
                .version(1)
                .build();
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(1L, "seungyong")
        );
    }
}
