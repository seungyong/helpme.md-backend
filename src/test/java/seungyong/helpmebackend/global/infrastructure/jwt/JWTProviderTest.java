package seungyong.helpmebackend.global.infrastructure.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.user.domain.entity.JWTUser;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JWTProviderTest {
    private final String secretKey = "dGhpc2lzYXZlcnlsb25nYW5kc2VjdXJland0c2VjcmV0a2V5MzJjaGFycw==";
    private JWTProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JWTProvider(secretKey);
    }

    @Nested
    @DisplayName("JWT 생성")
    class GenerateTests {
        @Test
        @DisplayName("실패 - 지원하지 않은 메소드")
        void generate_unsupported() {
            assertThatThrownBy(() -> jwtProvider.generate(1L))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Use generate(JWTUser user) instead.");
        }

        @Test
        @DisplayName("성공")
        void generate_success() {
            JWTUser user = new JWTUser(1L, "testuser");
            JWT jwt = jwtProvider.generate(user);

            assertThat(jwt).isNotNull().satisfies(it -> {
                assertThat(it.getGrantType()).isEqualTo("Bearer");
                assertThat(it.getAccessToken()).isNotEmpty();
                assertThat(it.getRefreshToken()).isNotEmpty();
                assertThat(it.getAccessTokenExpireTime()).isAfter(Instant.now());
                assertThat(it.getRefreshTokenExpireTime()).isAfter(Instant.now());
            });
        }
    }

    @Nested
    @DisplayName("유저 정보 조회")
    class getUserTests {
        @Test
        @DisplayName("성공")
        void getUserInfo_Success() {
            JWTUser user = new JWTUser(1L, "testuser");
            JWT jwt = jwtProvider.generate(user);

            JWTUser extractedUser = jwtProvider.getUserByToken(jwt.getAccessToken());

            assertThat(extractedUser).isNotNull()
                    .extracting(JWTUser::getId, JWTUser::getUsername)
                    .containsExactly(user.getId(), user.getUsername());
        }

        @Test
        @DisplayName("실패 - 만료된 토큰")
        void getUserInfo_failure_expiredToken() {
            String expiredToken = Jwts.builder()
                    .setSubject("1")
                    .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 이미 만료된 토큰
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                    .compact();

            assertThatThrownBy(() -> jwtProvider.getUserByToken(expiredToken))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.EXPIRED_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("실패 - null")
        void getUserInfo_failure_nullToken() {
            assertThatThrownBy(() -> jwtProvider.getUserByToken(null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.INVALID_TOKEN);
        }
    }

    @Nested
    @DisplayName("JWT 만료 여부 확인")
    class IsExpiredTests {
        @Test
        @DisplayName("만료된 토큰 - true")
        void isExpired_expiredToken() {
            String expiredToken = Jwts.builder()
                    .setSubject("1")
                    .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 이미 만료된 토큰
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                    .compact();

            boolean result = jwtProvider.isExpired(expiredToken, new Date());
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("유효한 토큰 - false")
        void isExpired_validToken() {
            String validToken = Jwts.builder()
                    .setSubject("1")
                    .setExpiration(new Date(System.currentTimeMillis() + 100000)) // 유효한 토큰
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                    .compact();

            boolean result = jwtProvider.isExpired(validToken, new Date());
            assertThat(result).isFalse();
        }
    }
}
