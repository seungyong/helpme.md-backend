package seungyong.helpmebackend.auth.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.auth.application.port.out.OAuth2PortOut;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private OAuth2PortOut oAuth2PortOut;
    @Mock private RedisPortOut redisPortOut;
    @Mock private JWTPortOut jwtPortOut;
    @Mock private AuthenticatedUserWriter authenticatedUserWriter;

    @InjectMocks private AuthService authService;

    @Nested
    @DisplayName("generateLoginUrl - 로그인 URL 생성")
    class GenerateLoginUrl {
        @Test
        @DisplayName("성공")
        void generateLoginUrl_success() {
            String expectedUrl = "https://github.com/login/oauth/authorize";
            given(redisPortOut.exists(anyString())).willReturn(false);
            given(oAuth2PortOut.generateLoginUrl(anyString())).willReturn(expectedUrl);

            String result = authService.generateLoginUrl();

            assertThat(result).isEqualTo(expectedUrl);
            verify(redisPortOut, times(1)).set(anyString(), eq("valid"), any(Instant.class));
        }

        @Test
        @DisplayName("성공 (중복 State 발생 시 재시도 로직 검증)")
        void generateLoginUrl_success_retryOnDuplicateState() {
            String expectedUrl = "https://github.com/login/oauth/authorize";
            given(redisPortOut.exists(anyString())).willReturn(true, false);
            given(oAuth2PortOut.generateLoginUrl(anyString())).willReturn(expectedUrl);

            String result = authService.generateLoginUrl();

            assertThat(result).isEqualTo(expectedUrl);

            verify(redisPortOut, times(2)).exists(anyString());
            verify(redisPortOut, times(1)).set(anyString(), eq("valid"), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("signupOrLogin - 회원가입 및 로그인")
    class SignupOrLogin {
        @Test
        @DisplayName("성공 (신규 유저)")
        void signupOrLogin_success_newUser() {
            String code = "auth-code";
            String state = "valid-state";
            OAuthTokenResult tokenResult = oauthTokenResult();
            OAuthGithubUser githubUser = oauthGithubUser();
            JWT expectedJwt = jwt();

            User mockUser = mock(User.class);
            GithubUser mockGithubUser = mock(GithubUser.class);

            given(redisPortOut.exists(anyString())).willReturn(true);
            given(oAuth2PortOut.getAccessToken(code)).willReturn(tokenResult);
            given(oAuth2PortOut.getGithubUser(tokenResult.accessToken())).willReturn(githubUser);
            given(authenticatedUserWriter.authenticate(
                    eq(githubUser), eq(tokenResult.accessToken()), any()
            )).willReturn(mockUser);

            given(mockUser.getId()).willReturn(1L);
            given(mockUser.getGithubUser()).willReturn(mockGithubUser);
            given(mockGithubUser.getName()).willReturn(githubUser.name());

            given(jwtPortOut.generate(any())).willReturn(expectedJwt);

            JWT result = authService.signupOrLogin(code, state);

            assertThat(result).isEqualTo(expectedJwt);
            verify(redisPortOut).delete(anyString());
            verify(authenticatedUserWriter).authenticate(
                    eq(githubUser), eq(tokenResult.accessToken()), any()
            );
            verify(redisPortOut).set(anyString(), eq("1"), any(Instant.class));
        }

        @Test
        @DisplayName("성공 (기존 활성 사용자의 로그인 정보 갱신)")
        void signupOrLogin_success_existingActiveUser() {
            String code = "auth-code";
            String state = "valid-state";
            OAuthTokenResult tokenResult = oauthTokenResult();
            OAuthGithubUser githubUser = oauthGithubUser();
            JWT expectedJwt = jwt();

            User mockUser = mock(User.class);
            GithubUser mockGithubUser = mock(GithubUser.class);

            given(redisPortOut.exists(anyString())).willReturn(true);
            given(oAuth2PortOut.getAccessToken(code)).willReturn(tokenResult);
            given(oAuth2PortOut.getGithubUser(tokenResult.accessToken())).willReturn(githubUser);
            given(authenticatedUserWriter.authenticate(
                    eq(githubUser), eq(tokenResult.accessToken()), any()
            )).willReturn(mockUser);
            given(mockUser.getId()).willReturn(1L);
            given(mockUser.getGithubUser()).willReturn(mockGithubUser);
            given(mockGithubUser.getName()).willReturn(githubUser.name());

            given(jwtPortOut.generate(any())).willReturn(expectedJwt);

            authService.signupOrLogin(code, state);

            verify(authenticatedUserWriter).authenticate(
                    eq(githubUser), eq(tokenResult.accessToken()), any()
            );
        }

        @Test
        @DisplayName("실패 (탈퇴 처리 중인 기존 사용자는 토큰을 발급하지 않음)")
        void signupOrLogin_failure_existingDeletingUser() {
            String code = "auth-code";
            String state = "valid-state";

            OAuthTokenResult tokenResult = oauthTokenResult();
            OAuthGithubUser githubUser = oauthGithubUser();

            given(redisPortOut.exists(anyString())).willReturn(true);
            given(oAuth2PortOut.getAccessToken(code)).willReturn(tokenResult);
            given(oAuth2PortOut.getGithubUser(tokenResult.accessToken())).willReturn(githubUser);

            given(authenticatedUserWriter.authenticate(
                    eq(githubUser), eq(tokenResult.accessToken()), any()
            )).willThrow(new CustomException(UserErrorCode.USER_DELETION_IN_PROGRESS));

            assertThatThrownBy(() -> authService.signupOrLogin(code, state))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_DELETION_IN_PROGRESS);

            verify(jwtPortOut, never()).generate(any());
        }

        @Test
        @DisplayName("실패 (유효하지 않은 State)")
        void signupOrLogin_failure_invalidState() {
            String code = "auth-code";
            String state = "invalid-state";

            given(redisPortOut.exists(anyString())).willReturn(false);

            assertThatThrownBy(() -> authService.signupOrLogin(code, state))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INVALID_OAUTH2_STATE);
        }
    }
}
