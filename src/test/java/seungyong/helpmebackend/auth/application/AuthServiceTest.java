package seungyong.helpmebackend.auth.application;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.auth.adapter.in.web.dto.response.ResponseInstallations;
import seungyong.helpmebackend.auth.application.port.out.OAuth2PortOut;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.auth.domain.entity.Installation;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private OAuth2PortOut oAuth2PortOut;
    @Mock private RedisPortOut redisPortOut;
    @Mock private CipherPortOut cipherPortOut;
    @Mock private JWTPortOut jwtPortOut;
    @Mock private UserPortOut userPortOut;

    @InjectMocks private AuthService authService;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .build();

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
            String encryptedToken = "encrypted-token";

            OAuthTokenResult tokenResult = fixtureMonkey.giveMeOne(OAuthTokenResult.class);
            OAuthGithubUser githubUser = fixtureMonkey.giveMeOne(OAuthGithubUser.class);
            JWT expectedJwt = fixtureMonkey.giveMeOne(JWT.class);

            User mockUser = mock(User.class);
            GithubUser mockGithubUser = mock(GithubUser.class);

            given(redisPortOut.exists(anyString())).willReturn(true);
            given(oAuth2PortOut.getAccessToken(code)).willReturn(tokenResult);
            given(oAuth2PortOut.getGithubUser(tokenResult.accessToken())).willReturn(githubUser);
            given(cipherPortOut.encrypt(tokenResult.accessToken())).willReturn(encryptedToken);

            given(userPortOut.getByGithubId(githubUser.githubId())).willReturn(Optional.empty());
            given(userPortOut.save(any(User.class))).willReturn(mockUser);

            given(mockUser.isDiffToken(encryptedToken)).willReturn(false);
            given(mockUser.getId()).willReturn(1L);
            given(mockUser.getGithubUser()).willReturn(mockGithubUser);
            given(mockGithubUser.getName()).willReturn(githubUser.name());

            given(jwtPortOut.generate(any())).willReturn(expectedJwt);

            JWT result = authService.signupOrLogin(code, state);

            assertThat(result).isEqualTo(expectedJwt);
            verify(redisPortOut).delete(anyString());
            verify(userPortOut).save(any(User.class));
            verify(redisPortOut).set(anyString(), eq("1"), any(Instant.class));
        }

        @Test
        @DisplayName("성공 (기존 유저 - 토큰 변경됨)")
        void signupOrLogin_success_existingUser_diffToken() {
            String code = "auth-code";
            String state = "valid-state";
            String encryptedToken = "new-encrypted-token";

            OAuthTokenResult tokenResult = fixtureMonkey.giveMeOne(OAuthTokenResult.class);
            OAuthGithubUser githubUser = fixtureMonkey.giveMeOne(OAuthGithubUser.class);
            JWT expectedJwt = fixtureMonkey.giveMeOne(JWT.class);

            User mockUser = mock(User.class);
            GithubUser mockGithubUser = mock(GithubUser.class);

            given(redisPortOut.exists(anyString())).willReturn(true);
            given(oAuth2PortOut.getAccessToken(code)).willReturn(tokenResult);
            given(oAuth2PortOut.getGithubUser(tokenResult.accessToken())).willReturn(githubUser);
            given(cipherPortOut.encrypt(tokenResult.accessToken())).willReturn(encryptedToken);

            given(userPortOut.getByGithubId(githubUser.githubId())).willReturn(Optional.of(mockUser));

            given(mockUser.isDiffToken(encryptedToken)).willReturn(true);
            given(mockUser.getId()).willReturn(1L);
            given(mockUser.getGithubUser()).willReturn(mockGithubUser);
            given(mockGithubUser.getName()).willReturn(githubUser.name());

            given(jwtPortOut.generate(any())).willReturn(expectedJwt);

            authService.signupOrLogin(code, state);

            verify(mockUser).updateGithubToken(any(EncryptedToken.class));
            verify(userPortOut).save(mockUser);
        }

        @Test
        @DisplayName("성공 (기존 유저 - 토큰 동일함)")
        void signupOrLogin_success_existingUser_sameToken() {
            String code = "auth-code";
            String state = "valid-state";
            String encryptedToken = "same-encrypted-token";

            OAuthTokenResult tokenResult = fixtureMonkey.giveMeOne(OAuthTokenResult.class);
            OAuthGithubUser githubUser = fixtureMonkey.giveMeOne(OAuthGithubUser.class);
            JWT expectedJwt = fixtureMonkey.giveMeOne(JWT.class);

            User mockUser = mock(User.class);
            GithubUser mockGithubUser = mock(GithubUser.class);

            given(redisPortOut.exists(anyString())).willReturn(true);
            given(oAuth2PortOut.getAccessToken(code)).willReturn(tokenResult);
            given(oAuth2PortOut.getGithubUser(tokenResult.accessToken())).willReturn(githubUser);
            given(cipherPortOut.encrypt(tokenResult.accessToken())).willReturn(encryptedToken);

            given(userPortOut.getByGithubId(githubUser.githubId())).willReturn(Optional.of(mockUser));

            given(mockUser.isDiffToken(encryptedToken)).willReturn(false);
            given(mockUser.getId()).willReturn(1L);
            given(mockUser.getGithubUser()).willReturn(mockGithubUser);
            given(mockGithubUser.getName()).willReturn(githubUser.name());

            given(jwtPortOut.generate(any())).willReturn(expectedJwt);

            authService.signupOrLogin(code, state);

            verify(mockUser, never()).updateGithubToken(any());
            verify(userPortOut, never()).save(any());
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

    @Nested
    @DisplayName("getInstallations - 설치된 GitHub App 정보 조회")
    class GetInstallations {
        @Test
        @DisplayName("성공")
        void getInstallations_success() {
            Long userId = 1L;
            String decryptedToken = "decrypted-token";

            List<Installation> expectedInstallations = fixtureMonkey.giveMeBuilder(Installation.class)
                    .sampleList(2);

            User mockUser = mock(User.class);
            GithubUser mockGithubUser = mock(GithubUser.class);
            EncryptedToken mockToken = mock(EncryptedToken.class);

            given(userPortOut.getById(userId)).willReturn(mockUser);
            given(mockUser.getGithubUser()).willReturn(mockGithubUser);
            given(mockGithubUser.getGithubToken()).willReturn(mockToken);
            given(mockToken.value()).willReturn("encrypted-value");

            given(cipherPortOut.decrypt("encrypted-value")).willReturn(decryptedToken);
            given(oAuth2PortOut.getInstallations(decryptedToken)).willReturn(expectedInstallations);

            ResponseInstallations result = authService.getInstallations(userId);

            assertThat(result.installations()).isEqualTo(expectedInstallations);
            assertThat(result.installations()).hasSize(2);
        }
    }
}