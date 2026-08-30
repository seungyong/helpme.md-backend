package seungyong.helpmebackend.notion.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.notion.application.port.in.NotionPortIn;
import seungyong.helpmebackend.notion.application.port.in.command.StartNotionAuthorizationCommand;
import seungyong.helpmebackend.notion.application.port.in.result.NotionAuthorizationResult;
import seungyong.helpmebackend.notion.application.port.in.result.NotionCallbackResult;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = {NotionController.class, NotionOAuthCallbackController.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "frontend.url=https://frontend.example")
class NotionControllerTest {
    private static final Long USER_ID = 1L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotionPortIn notionPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("OAuth 시작은 authorization URL과 state 만료 시각을 반환")
    void authorize_success() throws Exception {
        given(notionPortIn.startAuthorization(any(StartNotionAuthorizationCommand.class)))
                .willReturn(new NotionAuthorizationResult(
                        "https://api.notion.com/oauth?state=test",
                        OffsetDateTime.parse("2026-08-23T10:10:00Z")
                ));

        mockMvc.perform(post("/api/v1/integrations/notion/oauth/authorize")
                        .with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"returnUrl\":\"/settings/integrations\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl")
                        .value("https://api.notion.com/oauth?state=test"))
                .andExpect(jsonPath("$.stateExpiresAt")
                        .value("2026-08-23T10:10:00Z"));
    }

    @Test
    @DisplayName("연결이 없으면 404가 아니라 connected false와 명시적인 null 필드를 반환")
    void getConnection_disconnected() throws Exception {
        given(notionPortIn.getConnection(USER_ID))
                .willReturn(NotionConnection.disconnected(USER_ID));

        mockMvc.perform(get("/api/v1/integrations/notion").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.status").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.workspace").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.defaultParentPage").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("callback 성공은 프론트 설정 화면으로 302 이동")
    void callback_success() throws Exception {
        given(notionPortIn.handleCallback("code", "state", null))
                .willReturn(NotionCallbackResult.success("/settings/integrations"));

        mockMvc.perform(get("/api/v1/integrations/notion/oauth/callback")
                        .param("code", "code")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://frontend.example/#/settings/integrations?notion=success"
                ));
    }

    @Test
    @DisplayName("callback state 오류도 HTTP 오류 대신 프론트가 해석할 302 query로 반환")
    void callback_error() throws Exception {
        given(notionPortIn.handleCallback(null, "expired", null))
                .willReturn(NotionCallbackResult.error(
                        "/settings/integrations", "AUTH_40001"
                ));

        mockMvc.perform(get("/api/v1/integrations/notion/oauth/callback")
                        .param("state", "expired"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://frontend.example/#/settings/integrations"
                                + "?notion=error&code=AUTH_40001"
                ));
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(USER_ID, "seungyong")
        );
    }
}
