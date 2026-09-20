package seungyong.helpmebackend.user.adapter.in.web;

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
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.support.config.TestSecurityConfig;
import seungyong.helpmebackend.user.application.port.in.UserDeletionPortIn;
import seungyong.helpmebackend.user.application.port.in.command.DeleteUserCommand;
import seungyong.helpmebackend.user.application.port.in.result.UserDeletionResult;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserDeletionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class UserDeletionControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserDeletionPortIn userDeletionPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("탈퇴 상태를 먼저 저장한 202와 sign_out을 반환하고 쿠키를 제거")
    void acceptsDeletion() throws Exception {
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        when(cookieUtil.getRefreshToken(any())).thenReturn("refresh-token");
        when(userDeletionPortIn.requestDeletion(any(DeleteUserCommand.class)))
                .thenReturn(new UserDeletionResult("deleting", requestedAt, "sign_out"));

        mockMvc.perform(delete("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                new CustomUserDetails(1L, "seungyong")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("deleting"))
                .andExpect(jsonPath("$.requestedAt").value("2026-09-14T10:00:00Z"))
                .andExpect(jsonPath("$.requiredAction").value("sign_out"));

        verify(cookieUtil).clearTokenCookie(any());
    }

    @Test
    @DisplayName("confirmed가 false면 상태를 바꾸지 않고 400")
    void rejectsUnconfirmedDeletion() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                new CustomUserDetails(1L, "seungyong")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":false}"))
                .andExpect(status().isBadRequest());

        verify(userDeletionPortIn, never()).requestDeletion(any());
    }
}
