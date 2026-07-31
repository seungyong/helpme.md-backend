package seungyong.helpmebackend.auth.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import seungyong.helpmebackend.auth.adapter.in.web.dto.response.ResponseInstallations;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.auth.application.port.in.AuthPortIn;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;

import java.io.IOException;

@Slf4j
@Tag(name = "OAuth2", description = "OAuth2 관련 API")
@RestController
@RequestMapping("/api/v1/oauth2")
@ResponseBody
@RequiredArgsConstructor
class AuthController {
    @Value("${frontend.url}")
    private String frontendUrl;

    private final AuthPortIn authPortIn;
    private final CookieUtil cookieUtil;

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
        String loginUrl = authPortIn.generateLoginUrl();
        response.sendRedirect(loginUrl);
    }

    @Operation(
            summary = "GitHub App 콜백 처리",
            description = """
                    GitHub App 설치 후 콜백을 처리합니다.
                    - State 검증을 수행합니다.
                    - OAuth2 인증 및 회원가입/로그인을 처리합니다.
                      - 성공 시, `Access Token`과 `Refresh Token`은 `HttpOnly Cookie`로 설정됩니다.
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
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "installation_id", required = false) String installationId,
            @RequestParam(value = "action_type", required = false) String actionType,
            @RequestParam(value = "setup_action", required = false) String setupAction,
        HttpServletResponse response
    ) throws IOException {
        String redirectUrl;
        boolean isInstallation = isInstallationCallback(installationId, actionType, setupAction);

        if (isInstallation) {
            redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/readme/select")
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
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "유저를 찾을 수 없습니다.",
                    errorCodeClasses = UserErrorCode.class,
                    errorCodes = { "USER_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "예기치 못한 서버 에러입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "INTERNAL_SERVER_ERROR", "GITHUB_ERROR" }
            )
    })
    @UserRoleApiErrors
    @GetMapping("/installations")
    public ResponseEntity<ResponseInstallations> getInstallation(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ResponseInstallations installation = authPortIn.getInstallations(userDetails.getUserId());
        return ResponseEntity.ok(installation);
    }

    @Operation(
            summary = "인증 상태 확인",
            description = "현재 사용자의 인증 상태를 확인합니다. 유효한 토큰이 있는 경우 204 No Content를 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "인증 상태 확인 성공"
                    )
            }
    )
    @UserRoleApiErrors
    @PostMapping("/check")
    public ResponseEntity<Void> checkAuth() {
        return ResponseEntity.noContent().build();
    }

    private String loginOrSignup(String code, String state, HttpServletResponse response) {
        try {
            JWT jwt = authPortIn.signupOrLogin(code, state);
            cookieUtil.setTokenCookie(response, jwt);

            return UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                    .build()
                    .toUriString();
        } catch (CustomException e) {
            if (e.getErrorCode() == UserErrorCode.USER_DELETION_IN_PROGRESS) {
                cookieUtil.clearTokenCookie(response);
                return frontendUrl
                        + "/#/oauth2/callback?error="
                        + UserErrorCode.USER_DELETION_IN_PROGRESS.getErrorCode()
                        + "&requiredAction=sign_out";
            }

            log.error("OAuth2 login/signup failed", e);
            return authenticationFailedRedirectUrl();
        } catch (Exception e) {
            log.error("OAuth2 login/signup failed", e);
            return authenticationFailedRedirectUrl();
        }
    }

    private String authenticationFailedRedirectUrl() {
        return UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("error", "authentication_failed")
                .build()
                .toUriString();
    }

    private boolean isInstallationCallback(String installationId, String actionType, String setupAction) {
        String action = StringUtils.hasText(actionType) ? actionType : setupAction;
        return StringUtils.hasText(installationId)
                && ("install".equals(action) || "update".equals(action));
    }
}
