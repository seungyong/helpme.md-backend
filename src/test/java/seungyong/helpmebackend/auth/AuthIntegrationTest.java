package seungyong.helpmebackend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import seungyong.helpmebackend.auth.application.port.out.OAuth2PortOut;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.auth.domain.entity.Installation;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.installations;
import static seungyong.helpmebackend.support.fixture.TestFixtures.oauthGithubUser;
import static seungyong.helpmebackend.support.fixture.TestFixtures.oauthTokenResult;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
class AuthIntegrationTest {
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoSpyBean private OAuth2PortOut oAuth2PortOut;

    @Autowired private RedisPortOut redisPortOut;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private JWTPortOut jwtPortOut;

    @AfterEach
    void cleanup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Nested
    @DisplayName("login - OAuth2 로그인 URL 생성 및 리다이렉트")
    class Login {
        @Test
        @DisplayName("성공")
        void login_success() throws Exception {
            MvcResult mvcResult = mockMvc.perform(get("/api/v1/oauth2/login"))
                    .andExpect(status().is3xxRedirection())
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn();

            String redirectUrl = mvcResult.getResponse().getRedirectedUrl();
            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).startsWith("https://github.com/login/oauth/authorize");

            UriComponents uriComponents = UriComponentsBuilder.fromUriString(redirectUrl).build();
            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();

            assertThat(queryParams.containsKey("client_id")).isTrue();
            assertThat(queryParams.getFirst("scope")).isEqualTo("read:user");
            assertThat(queryParams.containsKey("redirect_uri")).isTrue();
            assertThat(queryParams.containsKey("state")).isTrue();

            String state = queryParams.getFirst("state");
            String redisKey = RedisKey.OAUTH2_STATE_KEY.getValue() + state;

            boolean existsInRedis = redisPortOut.exists(redisKey);
            assertThat(existsInRedis).isTrue();
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
                            .param("action_type", "install"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost:3000/readme/select"))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("성공 (로그인/회원가입 동작)")
        void githubAppCallback_success_loginOrSignup() throws Exception {
            String code = "test-code";
            String state = "test-state";

            // 실제 Redis에 State 값을 미리 넣어두어야 AuthService의 검증 로직 통과
            String stateKey = RedisKey.OAUTH2_STATE_KEY.getValue() + state;
            redisPortOut.set(stateKey, "valid", Instant.now().plus(10, ChronoUnit.MINUTES));

            OAuthTokenResult tokenResult = oauthTokenResult();
            OAuthGithubUser githubUser = oauthGithubUser();

            doReturn(tokenResult).when(oAuth2PortOut).getAccessToken(code);
            doReturn(githubUser).when(oAuth2PortOut).getGithubUser(tokenResult.accessToken());

            MvcResult result = mockMvc.perform(get("/api/v1/oauth2/callback")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn();

            String redirectUrl = result.getResponse().getRedirectedUrl();
            assertThat(redirectUrl).isNotNull().contains("/oauth2/callback");

            assertThat(redisPortOut.exists(stateKey)).isFalse();

            User savedUser = userPortOut.getByGithubId(githubUser.githubId()).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedUser.getGithubUser().getTokenStatus()).isEqualTo(GithubTokenStatus.VALID);
            assertThat(savedUser.getGithubUser().getTokenVerifiedAt()).isNotNull();
            assertThat(savedUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 (유효하지 않은 State)")
        void githubAppCallback_failure_invalidState() throws Exception {
            String code = "test-code";
            String state = "invalid-state";

            MvcResult result = mockMvc.perform(get("/api/v1/oauth2/callback")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn();

            String redirectUrl = result.getResponse().getRedirectedUrl();
            assertThat(redirectUrl).isNotNull().contains("?error=authentication_failed");
        }

        @Test
        @DisplayName("실패 (탈퇴 처리 중인 기존 사용자는 토큰 미발급 및 쿠키 만료)")
        void githubAppCallback_failure_userDeletionInProgress() throws Exception {
            String code = "test-code";
            String state = "test-state";
            long githubId = 987654L;

            User deletingUser = userPortOut.save(new User(
                    null,
                    new GithubUser(
                            "deleting-user",
                            githubId,
                            new EncryptedToken(cipherPortOut.encrypt("old-token"))
                    ),
                    UserStatus.DELETING
            ));

            String stateKey = RedisKey.OAUTH2_STATE_KEY.getValue() + state;
            redisPortOut.set(stateKey, "valid", Instant.now().plus(10, ChronoUnit.MINUTES));

            OAuthTokenResult tokenResult = new OAuthTokenResult("new-token", "bearer", "read:user");
            OAuthGithubUser githubUser = new OAuthGithubUser("deleting-user", githubId);
            doReturn(tokenResult).when(oAuth2PortOut).getAccessToken(code);
            doReturn(githubUser).when(oAuth2PortOut).getGithubUser(tokenResult.accessToken());

            mockMvc.perform(get("/api/v1/oauth2/callback")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(
                            "http://localhost:3000/#/oauth2/callback"
                                    + "?error=USER_40901&requiredAction=sign_out"
                    ))
                    .andExpect(cookie().maxAge("accessToken", 0))
                    .andExpect(cookie().maxAge("refreshToken", 0));

            User persistedUser = userPortOut.getByGithubId(githubId).orElseThrow();
            assertThat(persistedUser.getId()).isEqualTo(deletingUser.getId());
            assertThat(persistedUser.getStatus()).isEqualTo(UserStatus.DELETING);
            assertThat(persistedUser.getLastLoginAt()).isNull();
            assertThat(redisPortOut.exists(stateKey)).isFalse();
        }
    }

    @Nested
    @DisplayName("getInstallation - 접근 가능한 GitHub App 설치 정보 조회")
    class GetInstallation {
        @Test
        @DisplayName("성공")
        void getInstallation_success() throws Exception {
            String rawAccessToken = "real-github-token";
            String realEncryptedToken = cipherPortOut.encrypt(rawAccessToken);

            GithubUser githubUser = new GithubUser("test-user", 12345L, new EncryptedToken(realEncryptedToken));
            User user = new User(null, githubUser);
            User savedUser = userPortOut.save(user);

            JWT jwt = jwtPortOut.generate(new JWTUser(savedUser.getId(), savedUser.getGithubUser().getName()));

            CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
            given(mockUserDetails.getUserId()).willReturn(savedUser.getId());

            List<Installation> expectedInstallations = installations();
            doReturn(expectedInstallations).when(oAuth2PortOut).getInstallations(rawAccessToken);

            mockMvc.perform(get("/api/v1/oauth2/installations")
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.installations").isArray())
                    .andExpect(jsonPath("$.installations.length()").value(2))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("실패 (삭제된 사용자의 Access Token)")
        void getInstallation_failure_userNotFound() throws Exception {
            Long nonExistentUserId = 9999L;
            JWT jwt = jwtPortOut.generate(new JWTUser(nonExistentUserId, "nonexistent-user"));

            mockMvc.perform(get("/api/v1/oauth2/installations")
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value(GlobalErrorCode.INVALID_TOKEN.getErrorCode()))
                    .andExpect(cookie().maxAge("accessToken", 0))
                    .andExpect(cookie().maxAge("refreshToken", 0))
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @DisplayName("checkAuth - 인증 상태 확인")
    class CheckAuth {
        @Test
        @DisplayName("성공")
        void checkAuth_success() throws Exception {
            User savedUser = userPortOut.save(user(null, "check-auth-token"));
            JWT jwt = jwtPortOut.generate(new JWTUser(
                    savedUser.getId(),
                    savedUser.getGithubUser().getName()
            ));

            mockMvc.perform(post("/api/v1/oauth2/check")
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isNoContent())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("실패 (인증되지 않은 사용자)")
        void checkAuth_failure_unauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/oauth2/check"))
                    .andExpect(status().isUnauthorized())
                    .andDo(MockMvcResultHandlers.print());
        }
    }
}
