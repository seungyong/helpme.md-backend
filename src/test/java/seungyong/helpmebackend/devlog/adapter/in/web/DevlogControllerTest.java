package seungyong.helpmebackend.devlog.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import seungyong.helpmebackend.devlog.application.port.in.DevlogPortIn;
import seungyong.helpmebackend.devlog.application.port.in.command.SaveDevlogCommand;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = DevlogController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class DevlogControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;
    private static final LocalDate LOG_DATE = LocalDate.of(2026, 8, 23);

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DevlogPortIn devlogPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("작성하지 않은 날짜는 null 메타데이터를 포함한 exists=false 응답")
    void getDevlog_empty() throws Exception {
        given(devlogPortIn.getDevlog(USER_ID, PROJECT_ID, LOG_DATE))
                .willReturn(Devlog.empty(PROJECT_ID, LOG_DATE));

        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/devlogs/{logDate}",
                        PROJECT_ID,
                        LOG_DATE
                ).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.id").value(nullValue()))
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.logDate").value("2026-08-23"))
                .andExpect(jsonPath("$.contentMd").value(""))
                .andExpect(jsonPath("$.version").value(nullValue()))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));
    }

    @Test
    @DisplayName("개발로그 저장 요청을 version과 함께 command로 변환")
    void saveDevlog_success() throws Exception {
        given(devlogPortIn.saveDevlog(any(SaveDevlogCommand.class)))
                .willReturn(persisted("수정된 내용", 4));

        mockMvc.perform(put(
                        "/api/v1/projects/{projectId}/devlogs/{logDate}",
                        PROJECT_ID,
                        LOG_DATE
                ).with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentMd":"수정된 내용",
                                  "version":3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.id").value(301L))
                .andExpect(jsonPath("$.projectId").doesNotExist())
                .andExpect(jsonPath("$.contentMd").value("수정된 내용"))
                .andExpect(jsonPath("$.version").value(4));

        ArgumentCaptor<SaveDevlogCommand> captor =
                ArgumentCaptor.forClass(SaveDevlogCommand.class);
        verify(devlogPortIn).saveDevlog(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().projectId()).isEqualTo(PROJECT_ID);
        assertThat(captor.getValue().logDate()).isEqualTo(LOG_DATE);
        assertThat(captor.getValue().version()).isEqualTo(3);
    }

    @Test
    @DisplayName("contentMd가 없거나 날짜 형식이 잘못되면 REQ_400")
    void saveDevlog_invalidRequest() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/projects/{projectId}/devlogs/{logDate}",
                        PROJECT_ID,
                        LOG_DATE
                ).with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));

        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/devlogs/{logDate}",
                        PROJECT_ID,
                        "2026-13-40"
                ).with(user()))
                .andExpect(status().isBadRequest());

        verify(devlogPortIn, never()).saveDevlog(any());
    }

    @Test
    @DisplayName("version 충돌은 DOCUMENT_40901 응답")
    void saveDevlog_versionConflict() throws Exception {
        given(devlogPortIn.saveDevlog(any(SaveDevlogCommand.class)))
                .willThrow(new CustomException(
                        DocumentErrorCode.DOCUMENT_VERSION_CONFLICT
                ));

        mockMvc.perform(put(
                        "/api/v1/projects/{projectId}/devlogs/{logDate}",
                        PROJECT_ID,
                        LOG_DATE
                ).with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"오래된 수정","version":2}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENT_40901"));
    }

    private Devlog persisted(String content, int version) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-23T10:00:00Z");
        return new Devlog(301L, PROJECT_ID, LOG_DATE, content, version, now, now);
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(USER_ID, "seungyong")
        );
    }
}
