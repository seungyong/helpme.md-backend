package seungyong.helpmebackend.auth.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import seungyong.helpmebackend.auth.adapter.in.web.dto.response.ResponseInstallations;
import seungyong.helpmebackend.auth.application.port.in.AuthPortIn;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.support.config.TestSecurityConfig;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "frontend.url=https://test-frontend.com")
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthPortIn authPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Nested
    @DisplayName("login - OAuth2 로그인 URL 생성 및 리다이렉트")
    class Login {
        @Test
        @DisplayName("성공")
        void login_success() throws Exception {
            String expectedLoginUrl = "https://github.com/login/oauth/authorize?client_id=test-client-id&redirect_uri=https%3A%2F%2Ftest-frontend.com%2Foauth2%2Fcallback&state=test-state";
            given(authPortIn.generateLoginUrl()).willReturn(expectedLoginUrl);

            mockMvc.perform(get("/api/v1/oauth2/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(expectedLoginUrl));
        }
    }

    @Nested
    @DisplayName("githubAppCallback - GitHub App 콜백 처리")
    class GithubAppCallback {
        @Test
        @DisplayName("성공 (설치 동작)")
        void githubAppCallback_success_installation() throws Exception {
            mockMvc.perform(get("/api/v1/oauth2/callback")
                            .param("code", "test-code")
                            .param("installation_id", "12345")
                            .param("setup_action", "install"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("https://github.com/apps/helpme-md/installations/new"));
        }

        @Test
        @DisplayName("성공 (로그인/회원가입 동작)")
        void githubAppCallback_success_loginOrSignup() throws Exception {
            String code = "test-code";
            String state = "test-state";
            JWT jwt = fixtureMonkey.giveMeOne(JWT.class);

            given(authPortIn.signupOrLogin(code, state)).willReturn(jwt);

            mockMvc.perform(get("/api/v1/oauth2/callback")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("https://test-frontend.com/oauth2/callback"));

            verify(cookieUtil).setTokenCookie(any(HttpServletResponse.class), eq(jwt));
        }

        @Test
        @DisplayName("실패 (로그인 중 예외 발생)")
        void githubAppCallback_failure_loginException() throws Exception {
            String code = "test-code";
            String state = "test-state";

            given(authPortIn.signupOrLogin(code, state)).willThrow(new RuntimeException("Test Error"));

            mockMvc.perform(get("/api/v1/oauth2/callback")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("https://test-frontend.com/oauth2/callback?error=authentication_failed"));
        }
    }

    @Nested
    @DisplayName("getInstallation - 접근 가능한 GitHub App 설치 정보 조회")
    class GetInstallation {
        @Test
        @DisplayName("성공")
        void getInstallation_success() throws Exception {
            CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
            given(mockUserDetails.getUserId()).willReturn(1L);

            ResponseInstallations expectedResponse = fixtureMonkey.giveMeOne(ResponseInstallations.class);
            given(authPortIn.getInstallations(1L)).willReturn(expectedResponse);

            mockMvc.perform(get("/api/v1/oauth2/installations")
                            .with(user(mockUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
        }

        @Test
        @DisplayName("실패 (유저를 찾을 수 없음)")
        void getInstallation_failure_userNotFound() throws Exception {
            CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
            given(mockUserDetails.getUserId()).willReturn(999L);

            given(authPortIn.getInstallations(999L)).willThrow(new CustomException(UserErrorCode.USER_NOT_FOUND));

            mockMvc.perform(get("/api/v1/oauth2/installations")
                            .with(user(mockUserDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(UserErrorCode.USER_NOT_FOUND.getErrorCode()));
        }
    }

    @Nested
    @DisplayName("checkAuth - 인증 상태 확인")
    class CheckAuth {
        @Test
        @DisplayName("성공")
        void checkAuth_success() throws Exception {
            mockMvc.perform(post("/api/v1/oauth2/check"))
                    .andExpect(status().isNoContent());
        }
    }
}