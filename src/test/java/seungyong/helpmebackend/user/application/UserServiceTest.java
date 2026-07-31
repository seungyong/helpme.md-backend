package seungyong.helpmebackend.user.application;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import static org.assertj.core.api.Assertions.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.jwt;
import static seungyong.helpmebackend.support.fixture.TestFixtures.githubUser;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private RedisPortOut redisPortOut;
    @Mock private JWTPortOut jwtPortOut;
    @Mock private UserPortOut userPortOut;

    @InjectMocks private UserService userService;

    @Nested
    @DisplayName("활성 사용자 검증")
    class EnsureActiveUserTest {
        @Test
        @DisplayName("성공 - 활성 사용자")
        void ensureActiveUser_success() {
            Long userId = 1L;
            Mockito.when(userPortOut.getById(userId)).thenReturn(user(userId));

            assertThatCode(() -> userService.ensureActiveUser(userId))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = { "DELETING", "DELETE_FAILED" })
        @DisplayName("실패 - 탈퇴 처리 상태의 사용자")
        void ensureActiveUser_fail_when_user_deletion_in_progress(UserStatus status) {
            Long userId = 1L;
            Mockito.when(userPortOut.getById(userId))
                    .thenReturn(new User(userId, githubUser(), status));

            assertThatThrownBy(() -> userService.ensureActiveUser(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_DELETION_IN_PROGRESS);
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 사용자의 Access Token")
        void ensureActiveUser_fail_when_user_already_deleted() {
            Long userId = 99L;
            Mockito.when(userPortOut.getById(userId))
                    .thenThrow(new CustomException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userService.ensureActiveUser(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INVALID_TOKEN);
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class ReissueTest {
        @Test
        @DisplayName("성공 - 유효한 토큰")
        void reissue_success() {
            String refreshToken = "valid-refresh-token";
            String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + refreshToken;
            Long userId = 1L;
            User user = user(userId);
            JWT jwt = jwt("new-access-token", "new-refresh-token");

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

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = { "DELETING", "DELETE_FAILED" })
        @DisplayName("실패 - 탈퇴 처리 상태의 사용자는 토큰을 재발급하지 않음")
        void reissue_fail_when_user_deletion_in_progress(UserStatus status) {
            String refreshToken = "deleting-user-refresh-token";
            String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + refreshToken;
            Long userId = 1L;
            User user = new User(userId, githubUser(), status);

            Mockito.when(jwtPortOut.isExpired(Mockito.eq(refreshToken), Mockito.any()))
                    .thenReturn(false);
            Mockito.when(redisPortOut.get(refreshTokenKey))
                    .thenReturn(String.valueOf(userId));
            Mockito.when(userPortOut.getById(userId))
                    .thenReturn(user);

            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_DELETION_IN_PROGRESS);

            Mockito.verify(redisPortOut).delete(refreshTokenKey);
            Mockito.verify(jwtPortOut, Mockito.never()).generate(Mockito.any());
            Mockito.verify(redisPortOut, Mockito.never())
                    .set(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 사용자의 남은 Refresh Token은 무효화")
        void reissue_fail_when_user_already_deleted() {
            String refreshToken = "deleted-user-refresh-token";
            String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + refreshToken;
            Long deletedUserId = 99L;

            Mockito.when(jwtPortOut.isExpired(Mockito.eq(refreshToken), Mockito.any()))
                    .thenReturn(false);
            Mockito.when(redisPortOut.get(refreshTokenKey))
                    .thenReturn(String.valueOf(deletedUserId));
            Mockito.when(userPortOut.getById(deletedUserId))
                    .thenThrow(new CustomException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INVALID_TOKEN);

            Mockito.verify(redisPortOut).delete(refreshTokenKey);
            Mockito.verify(jwtPortOut, Mockito.never()).generate(Mockito.any());
        }
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() {
        Mockito
                .doNothing()
                .when(redisPortOut)
                .delete(Mockito.anyString());

        userService.logout("refresh-token");

        Mockito
                .verify(redisPortOut)
                .delete(Mockito.eq(RedisKey.REFRESH_KEY.getValue() + "refresh-token"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t" })
    @DisplayName("로그아웃 - Refresh Token이 없어도 성공")
    void logout_success_without_refresh_token(String refreshToken) {
        userService.logout(refreshToken);

        Mockito.verifyNoInteractions(redisPortOut);
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdraw_success() {
        Long userId = 1L;
        User user = user(userId, "valid-github-token");

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
