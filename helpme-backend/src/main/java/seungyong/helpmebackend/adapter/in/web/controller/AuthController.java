package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.oauth2.OAuth2PortIn;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@Tag(name = "OAuth2", description = "OAuth2 관련 API")
@RestController
@RequestMapping("/api/v1/oauth2")
@ResponseBody
@RequiredArgsConstructor
public class AuthController {
    private final OAuth2PortIn oAuth2PortIn;

    @Operation(
            summary = "GitHub OAuth2 콜백 처리",
            description = """
                    GitHub OAuth2 인증 후 콜백을 처리하고, JWT 토큰을 발급합니다.
                    - `code` 파라미터를 받아 GitHub 인증을 완료합니다.
                    - 발급된 `Access Token`을 사용해 사용자 정보를 조회합니다.
                    - 유저가 존재하지 않으면 회원가입을 진행합니다.
                    - 발급된 `Access Token`은 리다이렉트 URL의 쿼리 파라미터로 전달됩니다.
                    - 발급된 `Refresh Token`은 Redis 저장 및 HttpOnly 쿠키에 저장됩니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "302",
                            description = """
                                    리다이렉트 성공
                                    - `Access Token`이 쿼리 파라미터로 포함됩니다.
                                    - `Refresh Token`은 쿠키에 저장됩니다.
                                    """
                    )
            }
    )
    @ApiErrorResponses({
        @ApiErrorResponse(
                responseCode = "400",
                description = "잘못된 요청입니다.",
                errorCodeClass = GlobalErrorCode.class,
                errorCodes = { "BAD_REQUEST" }
        ),
        @ApiErrorResponse(
                responseCode = "500",
                description = "서버 에러입니다.",
                errorCodeClass = GlobalErrorCode.class,
                errorCodes = { "GITHUB_ERROR", "REDIS_ERROR", "INTERNAL_SERVER_ERROR" }
        )
    })
    @GetMapping("/github/callback")
    public void githubCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {
        JWT jwt = oAuth2PortIn.signupOrLogin(code);

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
                .queryParam("accessTokenExpireTime", jwt.getAccessTokenExpireTime().toString())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
