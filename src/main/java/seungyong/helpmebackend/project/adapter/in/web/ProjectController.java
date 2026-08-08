package seungyong.helpmebackend.project.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.project.adapter.in.web.dto.request.RequestProjectSettings;
import seungyong.helpmebackend.project.adapter.in.web.dto.response.ResponseProject;
import seungyong.helpmebackend.project.adapter.in.web.dto.response.ResponseProjectSettings;
import seungyong.helpmebackend.project.adapter.in.web.dto.response.ResponseUpdatedProjectSettings;
import seungyong.helpmebackend.project.application.port.in.ProjectPortIn;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;

@Tag(name = "Project", description = "프로젝트 core와 설정 API")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@UserRoleApiErrors
class ProjectController {
    private final ProjectPortIn projectPortIn;

    @Operation(
            summary = "프로젝트 상세 조회",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseProject.class)
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
    @GetMapping("/{projectId}")
    public ResponseEntity<ResponseProject> getProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(ResponseProject.from(
                projectPortIn.getProject(userDetails.getUserId(), projectId)
        ));
    }

    @Operation(
            summary = "프로젝트 설정 조회",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 설정 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseProjectSettings.class)
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
    @GetMapping("/{projectId}/settings")
    public ResponseEntity<ResponseProjectSettings> getProjectSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(ResponseProjectSettings.from(
                projectPortIn.getProjectSettings(userDetails.getUserId(), projectId)
        ));
    }

    @Operation(
            summary = "프로젝트 설정 수정",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 설정 수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseUpdatedProjectSettings.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "설정 형식 또는 값 범위가 올바르지 않습니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = "BAD_REQUEST"
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "GitHub Repository 접근 권한이 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_PERMISSION_DENIED"
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "선택한 GitHub Branch를 찾을 수 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RESOURCE_NOT_FOUND"
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
            )
    })
    @PatchMapping("/{projectId}/settings")
    public ResponseEntity<ResponseUpdatedProjectSettings> updateProjectSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody RequestProjectSettings request
    ) {
        return ResponseEntity.ok(ResponseUpdatedProjectSettings.from(
                projectPortIn.updateProjectSettings(
                        request.toCommand(userDetails.getUserId(), projectId)
                )
        ));
    }

}
