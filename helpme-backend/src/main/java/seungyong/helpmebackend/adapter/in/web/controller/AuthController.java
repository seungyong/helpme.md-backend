package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import seungyong.helpmebackend.adapter.in.web.dto.installation.response.ResponseInstallations;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.exception.UserErrorCode;
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
    @GetMapping("/callback")
    public void githubAppCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state") String state,
            @RequestParam(value = "installation_id", required = false) String installationId,
            @RequestParam(value = "setup_action", required = false) String setupAction,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl;
        boolean isInstallation = installationId != null && !installationId.isEmpty() && setupAction != null && !setupAction.isEmpty();

        if (isInstallation) {
            redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/callback")
                    .build()
                    .toUriString();
        }
        else { redirectUrl = loginOrSignup(code, state, response); }

        response.sendRedirect(redirectUrl);
    }

    @Operation(
            summary = "접근 가능한 GitHub App 설치 정보 조회",
            description = "사용자가 접근 가능한 GitHub App 설치 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "설치 정보 조회 성공",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ResponseInstallations.class)
                            )

                    )
            }
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    errorCodeClass = GlobalErrorCode.class,
                    errorCodes = { "UNAUTHORIZED", "INVALID_TOKEN", "EXPIRED_ACCESS_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "유저를 찾을 수 없습니다.",
                    errorCodeClass = UserErrorCode.class,
                    errorCodes = { "USER_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "예기치 못한 서버 에러입니다.",
                    errorCodeClass = GlobalErrorCode.class,
                    errorCodes = { "INTERNAL_SERVER_ERROR", "GITHUB_ERROR" }
            )
    })
    @GetMapping("/installation")
    public ResponseEntity<ResponseInstallations> getInstallation(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ResponseInstallations installation = oAuth2PortIn.getInstallations(userDetails.getUserId());
        return ResponseEntity.ok(installation);
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

            return UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/callback")
                    .queryParam("accessToken", jwt.getAccessToken())
                    .queryParam("accessTokenExpireTime", jwt.getAccessTokenExpireTime().toString())
                    .build()
                    .toUriString();
        } catch (Exception e) {
            log.error("OAuth2 login/signup failed", e);

            return UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/callback")
                    .queryParam("error", "authentication_failed")
                    .build()
                    .toUriString();
        }
    }
}
