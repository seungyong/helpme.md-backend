package seungyong.helpmebackend.adapter.in.web.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.infrastructure.jwt.JWT;

import java.time.Instant;

@Component
public class CookieUtil {
    public String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public void setTokenCookie(HttpServletResponse response, JWT jwt) {
        Instant now = Instant.now();

        long accessMaxAgeSeconds = jwt.getAccessTokenExpireTime().getEpochSecond() - now.getEpochSecond();
        long refreshMaxAgeSeconds = jwt.getRefreshTokenExpireTime().getEpochSecond() - now.getEpochSecond();

        // Cookie에 Access Token, Refresh Token 설정
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", jwt.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessMaxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", jwt.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshMaxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearTokenCookie(HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}
