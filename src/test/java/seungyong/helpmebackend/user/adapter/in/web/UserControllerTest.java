package seungyong.helpmebackend.user.adapter.in.web;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.support.config.TestSecurityConfig;
import seungyong.helpmebackend.user.application.port.in.UserPortIn;

@WebMvcTest(
        value = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
public class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserPortIn userPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("사용자 정보 조회 - 성공")
    void getCurrentUser_success() throws Exception {
        Long userId = 1L;
        String username = "test-user";
        CustomUserDetails userDetails = new CustomUserDetails(userId, username);

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/api/v1/users/me")
                                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(username));
    }

    @Nested
    @DisplayName("토큰 재발금")
    class ReissueToken {
        @Test
        @DisplayName("성공")
        void reissue_token_success() throws Exception {
            Long userId = 1L;
            String username = "test-user";
            CustomUserDetails userDetails = new CustomUserDetails(userId, username);

            String oldRefreshToken = "old-refresh-token";
            String newRefreshToken = "new-refresh-token";
            JWT jwt = FixtureMonkey.builder()
                    .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                    .defaultNotNull(true)
                    .build()
                    .giveMeBuilder(JWT.class)
                    .set("accessToken", "new-access-token")
                    .set("refreshToken", newRefreshToken)
                    .sample();

            Mockito
                    .when(cookieUtil.getRefreshToken(Mockito.any(HttpServletRequest.class)))
                    .thenReturn(oldRefreshToken);

            Mockito
                    .when(userPortIn.reissue(ArgumentMatchers.eq(oldRefreshToken)))
                    .thenReturn(jwt);

            mockMvc
                    .perform(
                            MockMvcRequestBuilders.post("/api/v1/users/reissue")
                                    .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isNoContent());

            assert jwt != null;
            Mockito
                        .verify(cookieUtil, Mockito.times(1))
                    .setTokenCookie(
                            Mockito.any(HttpServletResponse.class),
                            ArgumentMatchers.eq(jwt)
                    );
        }

        @Test
        @DisplayName("실패 - Refresh Token이 없는 경우")
        void reissue_token_failure() throws Exception {
            Long userId = 1L;
            String username = "test-user";
            CustomUserDetails userDetails = new CustomUserDetails(userId, username);

            Mockito
                    .when(cookieUtil.getRefreshToken(Mockito.any(HttpServletRequest.class)))
                    .thenReturn(null);

            mockMvc
                    .perform(
                            MockMvcRequestBuilders.post("/api/v1/users/reissue")
                                    .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(GlobalErrorCode.NOT_FOUND_TOKEN.name()));
        }
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() throws Exception {
        Long userId = 1L;
        String username = "test-user";
        CustomUserDetails userDetails = new CustomUserDetails(userId, username);

        Mockito
                .when(cookieUtil.getRefreshToken(Mockito.any(HttpServletRequest.class)))
                .thenReturn("refresh-token");

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/users/logout")
                                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        Mockito
                .verify(userPortIn, Mockito.times(1))
                .logout(ArgumentMatchers.eq(userId), Mockito.anyString());

        Mockito
                .verify(cookieUtil, Mockito.times(1))
                .getRefreshToken(Mockito.any(HttpServletRequest.class));

        Mockito
                .verify(cookieUtil, Mockito.times(1))
                .clearTokenCookie(Mockito.any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("회원탈퇴 - 성공")
    void withdraw_success() throws Exception {
        Long userId = 1L;
        String username = "test-user";
        CustomUserDetails userDetails = new CustomUserDetails(userId, username);

        Mockito
                .when(cookieUtil.getRefreshToken(Mockito.any(HttpServletRequest.class)))
                .thenReturn("refresh-token");

        mockMvc
                .perform(
                        MockMvcRequestBuilders.delete("/api/v1/users")
                                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        Mockito
                .verify(userPortIn, Mockito.times(1))
                .withdraw(ArgumentMatchers.eq(userId), Mockito.anyString());

        Mockito
                .verify(cookieUtil, Mockito.times(1))
                .getRefreshToken(Mockito.any(HttpServletRequest.class));

        Mockito
                .verify(cookieUtil, Mockito.times(1))
                .clearTokenCookie(Mockito.any(HttpServletResponse.class));
    }
}
