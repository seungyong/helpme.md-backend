package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import seungyong.helpmebackend.adapter.in.web.dto.user.response.ResponseUser;
import seungyong.helpmebackend.adapter.in.web.util.CookieUtil;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.user.UserPortIn;

import java.time.Instant;

@Tag(name = "User", description = "User 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@ResponseBody
@RequiredArgsConstructor
public class UserController {
    private final UserPortIn userPortIn;
    private final CookieUtil cookieUtil;

    @Operation(
            summary = "현재 사용자 정보 조회",
            description = "현재 인증된 사용자의 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "사용자 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseUser.class)
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
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ResponseUser> getUser(
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        ResponseUser responseUser = new ResponseUser(details.getUsername());
        return ResponseEntity.ok(responseUser);
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
                    errorCodes = { "INVALID_TOKEN", "NOT_FOUND_TOKEN" }
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
        String refreshToken = cookieUtil.getRefreshToken(request);

        if (refreshToken == null) {
            throw new CustomException(GlobalErrorCode.NOT_FOUND_TOKEN);
        }

        JWT jwt = userPortIn.reissue(refreshToken);
        cookieUtil.setTokenCookie(response, jwt);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 인증된 사용자의 로그아웃을 처리합니다. 이 작업은 클라이언트 측에서 저장된 토큰을 무효화합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "로그아웃 성공"
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
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
                HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        userPortIn.logout(details.getUserId(), cookieUtil.getRefreshToken(request));
        cookieUtil.clearTokenCookie(response);
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
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
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
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        String refreshTokenKey = cookieUtil.getRefreshToken(request);
        userPortIn.withdraw(details.getUserId(), refreshTokenKey);

        cookieUtil.clearTokenCookie(response);

        return ResponseEntity.noContent().build();
    }
}
