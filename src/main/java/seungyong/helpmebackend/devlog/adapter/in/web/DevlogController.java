package seungyong.helpmebackend.devlog.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.devlog.adapter.in.web.dto.request.RequestSaveDevlog;
import seungyong.helpmebackend.devlog.adapter.in.web.dto.response.ResponseDevlog;
import seungyong.helpmebackend.devlog.adapter.in.web.dto.response.ResponseSavedDevlog;
import seungyong.helpmebackend.devlog.application.port.in.DevlogPortIn;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;

import java.time.LocalDate;

@Tag(name = "Devlog", description = "프로젝트 개발로그 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/devlogs")
@RequiredArgsConstructor
@UserRoleApiErrors
class DevlogController {
    private final DevlogPortIn devlogPortIn;

    @Operation(
            summary = "개발로그 조회",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "개발로그 조회 성공. 작성하지 않은 날짜도 exists=false로 반환합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseDevlog.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한이 없습니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_ACCESS_DENIED"
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "프로젝트를 찾을 수 없습니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_NOT_FOUND"
            ),
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "프로젝트가 활성 상태가 아닙니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_NOT_ACTIVE"
            )
    })
    @GetMapping("/{logDate}")
    public ResponseEntity<ResponseDevlog> getDevlog(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate logDate
    ) {
        return ResponseEntity.ok(ResponseDevlog.from(
                devlogPortIn.getDevlog(userDetails.getUserId(), projectId, logDate)
        ));
    }

    @Operation(
            summary = "개발로그 저장",
            description = """
                    개발 로그 저장
                    version은 여러 곳에서 개발 로그의 상태를 변경할라고 하는 동시성 제어를 위해 사용됩니다.
                    
                    - 최초 저장은 version = null
                    - 수정·삭제는 조회한 현재 version을 전송
                    - 빈 contentMd는 저장하지 않거나, 기존 로그가 있으면 삭제 처리
                    """,
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "개발로그 저장 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseSavedDevlog.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "요청 형식 또는 version 범위가 올바르지 않습니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = "BAD_REQUEST"
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한이 없습니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_ACCESS_DENIED"
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "프로젝트를 찾을 수 없습니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_NOT_FOUND"
            ),
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "프로젝트가 활성 상태가 아닙니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_NOT_ACTIVE"
            ),
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "다른 변경 사항이 먼저 저장되었습니다.",
                    errorCodeClasses = DocumentErrorCode.class,
                    errorCodes = "DOCUMENT_VERSION_CONFLICT"
            )
    })
    @PutMapping("/{logDate}")
    public ResponseEntity<ResponseSavedDevlog> saveDevlog(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate logDate,
            @Valid @RequestBody RequestSaveDevlog request
    ) {
        return ResponseEntity.ok(ResponseSavedDevlog.from(
                devlogPortIn.saveDevlog(request.toCommand(
                        userDetails.getUserId(), projectId, logDate
                ))
        ));
    }
}
