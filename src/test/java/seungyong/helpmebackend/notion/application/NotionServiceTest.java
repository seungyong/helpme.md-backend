package seungyong.helpmebackend.notion.application;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.notion.application.port.in.command.StartNotionAuthorizationCommand;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.notion.application.port.out.NotionProviderPortOut;
import seungyong.helpmebackend.notion.application.port.out.exception.NotionProviderException;
import seungyong.helpmebackend.notion.application.port.out.result.NotionOAuthGrant;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPage;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPages;
import seungyong.helpmebackend.notion.application.port.out.result.NotionRefreshedTokens;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.notion.domain.exception.NotionErrorCode;
import seungyong.helpmebackend.notion.domain.type.NotionConnectionStatus;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotionServiceTest {
    private static final Long USER_ID = 1L;

    @Mock private RedisPortOut redisPortOut;
    @Mock private UserPortOut userPortOut;
    @Mock private NotionConnectionPortOut notionConnectionPortOut;
    @Mock private NotionProviderPortOut notionProviderPortOut;
    @Mock private CipherPortOut cipherPortOut;
    @Mock private User user;
    private NotionService service;

    @BeforeEach
    void setUp() {
        service = new NotionService(
                redisPortOut, userPortOut, notionConnectionPortOut,
                notionProviderPortOut, cipherPortOut
        );
        ReflectionTestUtils.setField(service, "allowedReturnUrl", "/settings/integrations");
        ReflectionTestUtils.setField(service, "stateTtlSeconds", 600L);
    }

    @Test
    @DisplayName("OAuth 시작은 사용자와 허용된 복귀 경로를 state에 저장")
    void startAuthorization_success() {
        given(userPortOut.getById(USER_ID)).willReturn(user);
        given(user.isAuthenticationAllowed()).willReturn(true);
        given(notionProviderPortOut.buildAuthorizationUrl(anyString()))
                .willAnswer(invocation -> "https://notion.test?state=" + invocation.getArgument(0));

        var result = service.startAuthorization(
                new StartNotionAuthorizationCommand(USER_ID, null)
        );

        assertThat(result.authorizationUrl()).startsWith("https://notion.test?state=");
        verify(redisPortOut).setObjectIfAbsent(
                org.mockito.ArgumentMatchers.startsWith("notion:oauth:state:"),
                any(), any(Instant.class)
        );
    }

    @Test
    @DisplayName("허용되지 않은 외부 returnUrl은 저장 전에 차단")
    void startAuthorization_openRedirectBlocked() {
        given(userPortOut.getById(USER_ID)).willReturn(user);
        given(user.isAuthenticationAllowed()).willReturn(true);

        assertThatThrownBy(() -> service.startAuthorization(
                new StartNotionAuthorizationCommand(USER_ID, "https://evil.example")
        )).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);

        verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any());
    }

    @Test
    @DisplayName("없거나 이미 소비된 state callback은 AUTH_40001로 복귀")
    void callback_invalidState() {
        given(redisPortOut.getObjectAndDelete(anyString(), any(TypeReference.class)))
                .willReturn(null);

        var result = service.handleCallback("code", "expired-state", null);

        assertThat(result.outcome().getQueryValue()).isEqualTo("error");
        assertThat(result.errorCode()).isEqualTo("AUTH_40001");
    }

    @Test
    @DisplayName("정상 callback은 state의 사용자를 재확인하고 token pair를 암호화해 저장")
    void callback_success() {
        given(userPortOut.getById(USER_ID)).willReturn(user);
        given(user.isAuthenticationAllowed()).willReturn(true);
        given(notionProviderPortOut.buildAuthorizationUrl(anyString()))
                .willReturn("https://notion.test/authorize");
        service.startAuthorization(new StartNotionAuthorizationCommand(USER_ID, null));

        ArgumentCaptor<Object> stateCaptor = ArgumentCaptor.forClass(Object.class);
        verify(redisPortOut).setObjectIfAbsent(anyString(), stateCaptor.capture(), any());
        given(redisPortOut.getObjectAndDelete(anyString(), any(TypeReference.class)))
                .willAnswer(invocation -> stateCaptor.getValue());
        given(notionProviderPortOut.exchangeAuthorizationCode("oauth-code"))
                .willReturn(new NotionOAuthGrant(
                        "access", "refresh", "bot-1", "workspace-1",
                        "Helpme", "Seungyong", "user@example.com"
                ));
        given(notionConnectionPortOut.getByUserId(USER_ID)).willReturn(Optional.empty());
        given(cipherPortOut.encrypt("access")).willReturn("encrypted-access");
        given(cipherPortOut.encrypt("refresh")).willReturn("encrypted-refresh");

        var result = service.handleCallback("oauth-code", "state", null);

        assertThat(result.outcome().getQueryValue()).isEqualTo("success");
        verify(notionConnectionPortOut).saveAuthorization(eq(USER_ID), any());
    }

    @Test
    @DisplayName("페이지 검색은 query·cursor·size를 한 번의 provider 호출로 전달")
    void getPages_queryAndPaginationForwardedOnce() {
        NotionConnection connection = connection();
        given(notionConnectionPortOut.getByUserId(USER_ID)).willReturn(Optional.of(connection));
        given(cipherPortOut.decrypt("encrypted-access")).willReturn("access");
        given(notionProviderPortOut.searchPages("access", "helpme", "cursor-1", 20))
                .willReturn(new NotionProviderPages(
                        List.of(new NotionProviderPage(
                                "page-1", "Helpme", OffsetDateTime.parse("2026-08-23T10:00:00Z")
                        )), "cursor-2", true
                ));

        var result = service.getPages(USER_ID, "helpme", "cursor-1", 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).path()).isEqualTo("Helpme Workspace / Helpme");
        verify(notionProviderPortOut, times(1))
                .searchPages("access", "helpme", "cursor-1", 20);
        verify(notionConnectionPortOut).recordVerification(eq(USER_ID), any());
    }

    @Test
    @DisplayName("access token 401은 token pair를 회전한 뒤 원 요청을 한 번 재시도")
    void getPages_refreshAndRetryOnce() {
        NotionConnection connection = connection();
        given(notionConnectionPortOut.getByUserId(USER_ID)).willReturn(Optional.of(connection));
        given(cipherPortOut.decrypt("encrypted-access")).willReturn("old-access");
        given(cipherPortOut.decrypt("encrypted-refresh")).willReturn("old-refresh");
        given(notionProviderPortOut.searchPages(
                anyString(), eq(null), eq(null), eq(30)
        )).willAnswer(invocation -> {
            if ("old-access".equals(invocation.getArgument(0))) {
                throw new NotionProviderException(NotionProviderException.Failure.UNAUTHORIZED);
            }
            return new NotionProviderPages(List.of(), null, false);
        });
        given(notionProviderPortOut.refreshAccessToken("old-refresh"))
                .willReturn(new NotionRefreshedTokens("new-access", "new-refresh"));
        given(cipherPortOut.encrypt("new-access")).willReturn("new-encrypted-access");
        given(cipherPortOut.encrypt("new-refresh")).willReturn("new-encrypted-refresh");

        service.getPages(USER_ID, null, null, null);

        verify(notionProviderPortOut).refreshAccessToken("old-refresh");
        verify(notionConnectionPortOut).rotateTokens(
                eq(USER_ID), eq("new-encrypted-access"), eq("new-encrypted-refresh"), any()
        );
        verify(notionProviderPortOut).searchPages("new-access", null, null, 30);
    }

    @Test
    @DisplayName("refresh token도 만료되면 재연결 필요 상태로 기록하고 NOTION_40302")
    void getPages_refreshExpired() {
        NotionConnection connection = connection();
        given(notionConnectionPortOut.getByUserId(USER_ID)).willReturn(Optional.of(connection));
        given(cipherPortOut.decrypt("encrypted-access")).willReturn("old-access");
        given(cipherPortOut.decrypt("encrypted-refresh")).willReturn("old-refresh");
        given(notionProviderPortOut.searchPages(anyString(), any(), any(), anyInt()))
                .willThrow(new NotionProviderException(NotionProviderException.Failure.UNAUTHORIZED));
        given(notionProviderPortOut.refreshAccessToken("old-refresh"))
                .willThrow(new NotionProviderException(NotionProviderException.Failure.UNAUTHORIZED));

        assertThatThrownBy(() -> service.getPages(USER_ID, null, null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", NotionErrorCode.NOTION_CONNECTION_EXPIRED
                );
        verify(notionConnectionPortOut).markReconnectRequired(
                eq(USER_ID), eq("NOTION_40302"), anyString(), any()
        );
    }

    private NotionConnection connection() {
        return NotionConnection.builder()
                .id(10L)
                .userId(USER_ID)
                .workspaceId("workspace-1")
                .workspaceName("Helpme Workspace")
                .encryptedAccessToken("encrypted-access")
                .encryptedRefreshToken("encrypted-refresh")
                .status(NotionConnectionStatus.CONNECTED)
                .build();
    }
}
