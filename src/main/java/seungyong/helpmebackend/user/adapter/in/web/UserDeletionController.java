package seungyong.helpmebackend.user.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.user.adapter.in.web.dto.request.RequestUserDeletion;
import seungyong.helpmebackend.user.adapter.in.web.dto.response.ResponseUserDeletion;
import seungyong.helpmebackend.user.application.port.in.UserDeletionPortIn;
import seungyong.helpmebackend.user.application.port.in.command.DeleteUserCommand;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserDeletionController {
    private final UserDeletionPortIn userDeletionPortIn;
    private final CookieUtil cookieUtil;

    @Operation(
            summary = "회원 탈퇴 요청",
            description = "사용자를 즉시 deleting으로 전환하고 백그라운드 정리를 시작합니다.",
            responses = @ApiResponse(responseCode = "202", description = "탈퇴 요청 접수")
    )
    @DeleteMapping("/me")
    public ResponseEntity<ResponseUserDeletion> deleteUser(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RequestUserDeletion request
    ) {
        ResponseUserDeletion response = ResponseUserDeletion.from(
                userDeletionPortIn.requestDeletion(new DeleteUserCommand(
                        userDetails.getUserId(),
                        Boolean.TRUE.equals(request.confirmed()),
                        cookieUtil.getRefreshToken(servletRequest)
                ))
        );
        cookieUtil.clearTokenCookie(servletResponse);
        return ResponseEntity.accepted().body(response);
    }
}
