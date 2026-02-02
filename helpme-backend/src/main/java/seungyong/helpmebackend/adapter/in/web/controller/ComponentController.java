package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import seungyong.helpmebackend.adapter.in.web.dto.component.request.RequestComponent;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseComponents;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseCreatedComponent;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.component.ComponentPortIn;

@Tag(
        name = "Component",
        description = """
                Component 관련 API
                """
)
@RestController
@RequestMapping("/api/v1")
@ResponseBody
@RequiredArgsConstructor
public class ComponentController {
    private final ComponentPortIn componentPortIn;

    @Operation(
            summary = "특정 Repository의 Component 목록 조회",
            description = """
                    특정 Repository의 Component 목록을 조회합니다.
                    - 컴포넌트가 존재하지 않는 경우, 빈 배열을 반환합니다.
                    - 페이지네이션이 필요하지 않습니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "잘못된 요청입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "BAD_REQUEST" }
            ),
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
    @GetMapping("/repos/{owner}/{name}/components")
    public ResponseEntity<ResponseComponents> getComponents(
            @PathVariable String owner,
            @PathVariable String name,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        return ResponseEntity.ok(
                componentPortIn.getComponents(owner, name, userDetails.getUserId())
        );
    }

    @Operation(
            summary = "특정 Repository의 Component 생성",
            description = """
                    특정 Repository에 Component를 생성합니다.
                    - 생성된 Component의 ID를 반환합니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "잘못된 요청입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "BAD_REQUEST" }
            ),
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
    @PostMapping("/repos/{owner}/{name}/components")
    public ResponseEntity<ResponseCreatedComponent> createComponent(
            @RequestBody @Valid RequestComponent request,
            @PathVariable String owner,
            @PathVariable String name,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                componentPortIn.createComponent(request, owner, name, userDetails.getUserId())
        );
    }

    @Operation(
            summary = "Component 수정",
            description = """
                    Component를 수정합니다.
                    - 성공 시, 응답 본문이 없는 204 No Content를 반환합니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "잘못된 요청입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "BAD_REQUEST" }
            ),
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
    @PatchMapping("/components/{componentId}")
    public ResponseEntity<Void> updateComponent(
            @RequestBody @Valid RequestComponent request,
            @PathVariable Long componentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        componentPortIn.updateComponent(request, componentId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Component 삭제",
            description = """
                    Component를 삭제합니다.
                    - 성공 시, 응답 본문이 없는 204 No Content를 반환합니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "잘못된 요청입니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = { "BAD_REQUEST" }
            ),
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
    @DeleteMapping("/components/{componentId}")
    public ResponseEntity<Void> deleteComponent(
            @PathVariable Long componentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        componentPortIn.deleteComponent(componentId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
