package seungyong.helpmebackend.global.infrastructure.cookie;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import seungyong.helpmebackend.global.domain.entity.JWT;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CookieUtilTest {
    @InjectMocks private CookieUtil cookieUtil;

    @Nested
    @DisplayName("Refresh Token 조회")
    class RefreshTokenTests {
        @Test
        @DisplayName("성공")
        void getRefreshToken_success() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            Cookie refreshTokenCookie = new Cookie("refreshToken", "test-refresh-token");
            request.setCookies(refreshTokenCookie);

            String refreshToken = cookieUtil.getRefreshToken(request);

            assertThat(refreshToken).isEqualTo("test-refresh-token");
        }

        @Test
        @DisplayName("실패 - Refresh Token 없음")
        void getRefreshToken_notFound() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            Cookie otherCookie = new Cookie("otherCookie", "other-value");
            request.setCookies(otherCookie);

            String refreshToken = cookieUtil.getRefreshToken(request);

            assertThat(refreshToken).isNull();
        }

        @Test
        @DisplayName("실패 - 쿠키 없음")
        void getRefreshToken_noCookies() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            String refreshToken = cookieUtil.getRefreshToken(request);

            assertThat(refreshToken).isNull();
        }
    }

    @Test
    @DisplayName("AT, RT 토큰 쿠키 설정 - 성공")
    void setTokenCookies_success() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        JWT jwt = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeBuilder(JWT.class)
                .set("grantType", "Bearer")
                .set("accessToken", "test-access-token")
                .set("refreshToken", "test-refresh-token")
                .sample();

        assert jwt != null;
        cookieUtil.setTokenCookie(response, jwt);

        List<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        log.info("Set-Cookie Headers: {}", setCookieHeaders);

        assertThat(setCookieHeaders).hasSize(2);

        assertThat(setCookieHeaders.get(0))
                .contains("accessToken=" + jwt.getAccessToken())
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");

        assertThat(setCookieHeaders.get(1))
                .contains("refreshToken=" + jwt.getRefreshToken())
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    @DisplayName("토큰 쿠키 삭제 - 성공")
    void clearTokenCookies_success() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.clearTokenCookie(response);

        List<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        log.info("Set-Cookie Headers: {}", setCookieHeaders);

        assertThat(setCookieHeaders).hasSize(2);

        assertThat(setCookieHeaders.get(0))
                .contains("accessToken=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");

        assertThat(setCookieHeaders.get(1))
                .contains("refreshToken=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }
}
