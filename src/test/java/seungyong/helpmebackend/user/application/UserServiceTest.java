package seungyong.helpmebackend.user.application;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private RedisPortOut redisPortOut;
    @Mock private JWTPortOut jwtPortOut;
    @Mock private UserPortOut userPortOut;

    @InjectMocks private UserService userService;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class ReissueTest {
        @Test
        @DisplayName("성공 - 유효한 토큰")
        void reissue_success() {
            String refreshToken = "valid-refresh-token";
            String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + refreshToken;
            Long userId = 1L;
            User user = fixtureMonkey.giveMeBuilder(User.class)
                    .set("id", userId)
                    .sample();
            JWT jwt = fixtureMonkey.giveMeBuilder(JWT.class)
                    .set("refreshToken", "new-refresh-token")
                    .set("refreshTokenExpireTime", Instant.now().plusSeconds(3600))
                    .sample();

            Mockito
                    .when(jwtPortOut.isExpired(Mockito.eq(refreshToken), Mockito.any()))
                    .thenReturn(false);

            Mockito
                    .when(redisPortOut.get(Mockito.eq(refreshTokenKey)))
                    .thenReturn(String.valueOf(userId));

            Mockito
                    .when(userPortOut.getById(Mockito.eq(userId)))
                    .thenReturn(user);

            Mockito
                    .when(jwtPortOut.generate(Mockito.any(JWTUser.class)))
                    .thenReturn(jwt);

            JWT result = userService.reissue(refreshToken);
            assert jwt != null;

            assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");

            Mockito
                    .verify(redisPortOut)
                    .set(
                            Mockito.eq(refreshTokenKey),
                            Mockito.eq(String.valueOf(userId)),
                            Mockito.eq(jwt.getRefreshTokenExpireTime())
                    );
        }

        @Test
        @DisplayName("실패 - 만료된 토큰")
        void reissue_fail_when_token_expired() {
            String token = "expired-token";

            Mockito
                    .when(jwtPortOut.isExpired(Mockito.eq(token), Mockito.any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> userService.reissue(token))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INVALID_TOKEN);
        }
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() {
        Mockito
                .doNothing()
                .when(redisPortOut)
                .delete(Mockito.anyString());

        userService.logout(1L, "refresh-token");

        Mockito
                .verify(redisPortOut)
                .delete(Mockito.eq(RedisKey.REFRESH_KEY.getValue() + "refresh-token"));
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdraw_success() {
        Long userId = 1L;
        User user = fixtureMonkey.giveMeBuilder(User.class)
                .set("id", userId)
                .set("githubUser.githubToken.value", "valid-github-token")
                .sample();

        Mockito
                .when(userPortOut.getById(Mockito.eq(userId)))
                .thenReturn(user);

        Mockito
                .doNothing()
                .when(userPortOut)
                .delete(Mockito.eq(user));

        Mockito
                .doNothing()
                .when(redisPortOut)
                .delete(Mockito.anyString());

        userService.withdraw(1L, "refresh-token");

        Mockito
                .verify(userPortOut)
                .delete(Mockito.eq(user));

        Mockito
                .verify(redisPortOut)
                .delete(Mockito.eq(RedisKey.REFRESH_KEY.getValue() + "refresh-token"));
    }
}
