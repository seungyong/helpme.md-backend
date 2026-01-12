package seungyong.helpmebackend.adapter.in.web.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.usecase.port.in.github.GithubPortIn;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/oauth2")
@ResponseBody
@RequiredArgsConstructor
public class AuthController {
    private final GithubPortIn githubService;

    @GetMapping("/github/callback")
    public void githubCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {
        JWT jwt = githubService.signupOrLogin(code);

        Instant now = Instant.now();
        Instant expire = jwt.getRefreshTokenExpireTime().toInstant(ZoneOffset.UTC);
        long maxAgeSeconds = Duration.between(now, expire).getSeconds();

        // Cookie에 Refresh Token 설정
        ResponseCookie cookie = ResponseCookie.from("refreshToken", jwt.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/github/callback")
                .queryParam("accessToken", jwt.getAccessToken())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
