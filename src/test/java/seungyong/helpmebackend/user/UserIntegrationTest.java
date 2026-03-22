package seungyong.helpmebackend.user;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.global.adapter.out.redis.RedisAdapter;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
public class UserIntegrationTest {
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @Autowired private MockMvc mockMvc;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private RedisAdapter redisAdapter;
    @Autowired private UserPortOut userPortOut;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @AfterEach
    void cleanup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("내 정보 조회 - 성공")
    void getCurrentUser_success() throws Exception {
        Long userId = 1L;
        String username = "test-user";
        JWT jwt = jwtProvider.generate(new JWTUser(userId, username));

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/api/v1/users/me")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                )
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(username));
    }

    @Nested
    @DisplayName("토큰 재발급")
    class ReissueTests {
        @Test
        @DisplayName("성공")
        void reissue_success() throws Exception {
            User user = fixtureMonkey.giveMeBuilder(User.class)
                    .setNull("id")
                    .set("githubUser.githubToken.value", "test")
                    .sample();
            User savedUser = userPortOut.save(user);

            JWT jwt = jwtProvider.generate(new JWTUser(savedUser.getId(), savedUser.getGithubUser().getName()));

            String refreshKey = RedisKey.REFRESH_KEY.getValue() + jwt.getRefreshToken();
            redisAdapter.set(refreshKey, String.valueOf(savedUser.getId()), jwt.getRefreshTokenExpireTime());

            mockMvc
                    .perform(
                            MockMvcRequestBuilders.post("/api/v1/users/reissue")
                                    .cookie(
                                            new Cookie("refreshToken", jwt.getRefreshToken())
                                    )
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isNoContent())
                    .andExpect(MockMvcResultMatchers.cookie().exists("accessToken"))
                    .andExpect(MockMvcResultMatchers.cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("실패 - 리프레시 토큰 만료")
        void reissue_fail_expiredRefreshToken() throws Exception {
            User user = fixtureMonkey.giveMeBuilder(User.class)
                    .setNull("id")
                    .set("githubUser.githubToken.value", "test")
                    .sample();
            User savedUser = userPortOut.save(user);

            JWT jwt = jwtProvider.generate(new JWTUser(savedUser.getId(), savedUser.getGithubUser().getName()));

            mockMvc
                    .perform(
                            MockMvcRequestBuilders.post("/api/v1/users/reissue")
                                    .cookie(
                                            new Cookie("refreshToken", jwt.getRefreshToken())
                                    )
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(GlobalErrorCode.INVALID_TOKEN.name()));
        }

        @Test
        @DisplayName("실패 - 리프레시 토큰 없음")
        void reissue_fail_noRefreshToken() throws Exception {
            mockMvc
                    .perform(
                            MockMvcRequestBuilders.post("/api/v1/users/reissue")
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(GlobalErrorCode.NOT_FOUND_TOKEN.name()));
        }

        @Test
        @DisplayName("실패 - 리프레시 토큰 유효하지 않음")
        void reissue_fail_invalidRefreshToken() throws Exception {
            mockMvc
                    .perform(
                            MockMvcRequestBuilders.post("/api/v1/users/reissue")
                                    .cookie(
                                            new Cookie("refreshToken", "invalid-refresh-token")
                                    )
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(GlobalErrorCode.INVALID_TOKEN.name()));
        }
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() throws Exception {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test")
                .sample();
        User savedUser = userPortOut.save(user);

        JWT jwt = jwtProvider.generate(new JWTUser(savedUser.getId(), savedUser.getGithubUser().getName()));

        String refreshKey = RedisKey.REFRESH_KEY.getValue() + jwt.getRefreshToken();
        redisAdapter.set(refreshKey, String.valueOf(savedUser.getId()), jwt.getRefreshTokenExpireTime());

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/api/v1/users/logout")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                )
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdraw_success() throws Exception {
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .setNull("id")
                .set("githubUser.githubToken.value", "test")
                .sample();
        User savedUser = userPortOut.save(user);

        JWT jwt = jwtProvider.generate(new JWTUser(savedUser.getId(), savedUser.getGithubUser().getName()));

        String refreshKey = RedisKey.REFRESH_KEY.getValue() + jwt.getRefreshToken();
        redisAdapter.set(refreshKey, String.valueOf(savedUser.getId()), jwt.getRefreshTokenExpireTime());

        mockMvc
                .perform(
                        MockMvcRequestBuilders.delete("/api/v1/users")
                                .cookie(
                                        new Cookie("accessToken", jwt.getAccessToken()),
                                        new Cookie("refreshToken", jwt.getRefreshToken())
                                )
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge("refreshToken", 0));

        assertThatThrownBy(() -> userPortOut.getById(savedUser.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }
}
