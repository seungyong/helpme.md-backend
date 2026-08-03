package seungyong.helpmebackend.user;

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
import seungyong.helpmebackend.user.domain.type.UserStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.githubUser;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

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

    @AfterEach
    void cleanup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("내 정보 조회 - 성공")
    void getCurrentUser_success() throws Exception {
        User savedUser = userPortOut.save(user(null, "test"));
        String username = savedUser.getGithubUser().getName();
        JWT jwt = jwtProvider.generate(new JWTUser(savedUser.getId(), username));

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
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(savedUser.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(username))
                .andExpect(MockMvcResultMatchers.jsonPath("$.githubId").value(1001L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.plan.code").value("free"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.plan.projectLimit").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("active"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.githubTokenStatus").value("valid"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("User Role API - 탈퇴 처리 중이면 공통 guard가 차단하고 로그아웃을 지시")
    void userRoleApi_fail_user_deletion_in_progress() throws Exception {
        User deletingUser = userPortOut.save(new User(null, githubUser(), UserStatus.DELETING));
        JWT jwt = jwtProvider.generate(new JWTUser(
                deletingUser.getId(),
                deletingUser.getGithubUser().getName()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/repos")
                        .param("installation_id", "101")
                        .cookie(
                                new Cookie("accessToken", jwt.getAccessToken()),
                                new Cookie("refreshToken", jwt.getRefreshToken())
                        ))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode")
                        .value(UserErrorCode.USER_DELETION_IN_PROGRESS.getErrorCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.requiredAction").value("sign_out"))
                .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge("refreshToken", 0));
    }

    @Nested
    @DisplayName("토큰 재발급")
    class ReissueTests {
        @Test
        @DisplayName("성공")
        void reissue_success() throws Exception {
            User user = user(null, "test");
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
            User user = user(null, "test");
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

        @Test
        @DisplayName("실패 - 탈퇴 처리 중인 사용자는 토큰 무효화 및 쿠키 제거")
        void reissue_fail_user_deletion_in_progress() throws Exception {
            User deletingUser = userPortOut.save(new User(null, githubUser(), UserStatus.DELETING));
            JWT jwt = jwtProvider.generate(new JWTUser(
                    deletingUser.getId(),
                    deletingUser.getGithubUser().getName()
            ));
            String refreshKey = RedisKey.REFRESH_KEY.getValue() + jwt.getRefreshToken();
            redisAdapter.set(refreshKey, String.valueOf(deletingUser.getId()), jwt.getRefreshTokenExpireTime());

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/reissue")
                            .cookie(new Cookie("refreshToken", jwt.getRefreshToken())))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode")
                            .value(UserErrorCode.USER_DELETION_IN_PROGRESS.getErrorCode()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.requiredAction").value("sign_out"))
                    .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
                    .andExpect(MockMvcResultMatchers.cookie().maxAge("refreshToken", 0));

            assertThat(redisAdapter.get(refreshKey)).isNull();
        }
    }

    @Test
    @DisplayName("로그아웃 - 성공 및 반복 호출")
    void logout_success() throws Exception {
        User user = user(null, "test");
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

        assertThat(redisAdapter.get(refreshKey)).isNull();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/logout")
                        .cookie(new Cookie("refreshToken", jwt.getRefreshToken())))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge("refreshToken", 0));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/logout"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("로그아웃 - 탈퇴 처리 중인 사용자도 성공")
    void logout_success_user_deletion_in_progress() throws Exception {
        User deletingUser = userPortOut.save(new User(null, githubUser(), UserStatus.DELETING));
        JWT jwt = jwtProvider.generate(new JWTUser(
                deletingUser.getId(),
                deletingUser.getGithubUser().getName()
        ));
        String refreshKey = RedisKey.REFRESH_KEY.getValue() + jwt.getRefreshToken();
        redisAdapter.set(refreshKey, String.valueOf(deletingUser.getId()), jwt.getRefreshTokenExpireTime());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/logout")
                        .cookie(new Cookie("refreshToken", jwt.getRefreshToken())))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge("refreshToken", 0));

        assertThat(redisAdapter.get(refreshKey)).isNull();
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdraw_success() throws Exception {
        User user = user(null, "test");
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
