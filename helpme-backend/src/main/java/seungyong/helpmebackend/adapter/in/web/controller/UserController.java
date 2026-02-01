package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.user.UserPortIn;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@Tag(name = "User", description = "User 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@ResponseBody
@RequiredArgsConstructor
public class UserController {
    private final UserPortIn userPortIn;

    private String getToken(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    @Operation(
            summary = "토큰 재발급",
            description = "만료된 Access Token과 유효한 Refresh Token을 사용하여 새로운 JWT 토큰을 발급합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "토큰 재발급 성공"
                    )
            }
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "UNAUTHORIZED", "INVALID_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = getToken(request, "refreshToken");

        if (refreshToken == null) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        JWT jwt = userPortIn.reissue(refreshToken);

        // 쿠키 설정
        Instant now = Instant.now();
        long accessMaxAgeSeconds = jwt.getAccessTokenExpireTime().getEpochSecond() - now.getEpochSecond();
        long refreshMaxAgeSeconds = jwt.getRefreshTokenExpireTime().getEpochSecond() - now.getEpochSecond();

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", jwt.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessMaxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", jwt.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshMaxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 인증된 사용자의 회원 탈퇴를 처리합니다. 이 작업은 사용자의 데이터를 영구적으로 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "회원 탈퇴 성공"
                    )
            }
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "UNAUTHORIZED", "INVALID_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        userPortIn.withdraw(details.getUserId());

        // 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }
}
