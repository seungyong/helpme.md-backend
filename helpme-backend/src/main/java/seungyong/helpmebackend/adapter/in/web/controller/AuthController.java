package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.oauth2.OAuth2PortIn;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@Slf4j
@Tag(name = "OAuth2", description = "OAuth2 관련 API")
@RestController
@RequestMapping("/api/v1/oauth2")
@ResponseBody
@RequiredArgsConstructor
public class AuthController {
    private final OAuth2PortIn oAuth2PortIn;

    @Operation(
            summary = "OAuth2 로그인 URL 생성 및 리다이렉트",
            description = """
                    OAuth2 로그인 URL을 생성하고 해당 URL로 리다이렉트합니다.
                    - 랜덤한 `State`를 생성하여 Redis에 저장합니다. (10분)
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "302",
                            description = "리다이렉트 성공"
                    )
            }
    )
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        String loginUrl = oAuth2PortIn.generateLoginUrl();
        response.sendRedirect(loginUrl);
    }

    @Operation(
            summary = "GitHub App 콜백 처리",
            description = """
                    GitHub App 설치 후 콜백을 처리합니다.
                    - State 검증을 수행합니다.
                    - OAuth2 인증 및 회원가입/로그인을 처리합니다.
                      - 성공 시, `Access Token`을 쿼리 파라미터로 전달하여 리다이렉트합니다.
                        - `Refresh Token`은 `HttpOnly Cookie`로 설정됩니다.
                      - 실패 시, 에러 정보를 쿼리 파라미터(`authentication_failed`)로 전달하여 리다이렉트합니다.
                    - GitHub App 설치를 처리합니다.
                        - 레포 선택 페이지로 리다이렉트합니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "302",
                            description = "리다이렉트 성공"
                    )
            }
    )
    @GetMapping("/github/installation")
    public void githubAppInstallationCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state") String state,
            @RequestParam(value = "installation_id", required = false) String installationId,
            @RequestParam(value = "setup_action", required = false) String setupAction,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl;
        boolean isInstallation = installationId != null && !installationId.isEmpty() && setupAction != null && !setupAction.isEmpty();

        if (isInstallation) {
            redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/github/installation")
                    .build()
                    .toUriString();
        }
        else { redirectUrl = loginOrSignup(code, state, response); }

        response.sendRedirect(redirectUrl);
    }

    private String loginOrSignup(String code, String state, HttpServletResponse response) {
        try {
            JWT jwt = oAuth2PortIn.signupOrLogin(code, state);

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

            return UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/github/callback")
                    .queryParam("accessToken", jwt.getAccessToken())
                    .queryParam("accessTokenExpireTime", jwt.getAccessTokenExpireTime().toString())
                    .build()
                    .toUriString();
        } catch (Exception e) {
            log.error("OAuth2 login/signup failed", e);

            return UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/github/installation")
                    .queryParam("error", "authentication_failed")
                    .build()
                    .toUriString();
        }
    }
}
