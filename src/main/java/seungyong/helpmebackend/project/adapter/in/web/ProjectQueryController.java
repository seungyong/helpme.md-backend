package seungyong.helpmebackend.project.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.project.adapter.in.web.dto.response.ResponseProjectOverview;
import seungyong.helpmebackend.project.adapter.in.web.dto.response.ResponseProjects;
import seungyong.helpmebackend.project.application.port.in.ProjectQueryPortIn;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;

@Tag(name = "Project", description = "프로젝트 core와 집계 조회 API")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@UserRoleApiErrors
class ProjectQueryController {
    private final ProjectQueryPortIn projectQueryPortIn;

    @Operation(
            summary = "프로젝트 목록 조회",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "플랜과 프로젝트별 최근 집계 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseProjects.class)
                    )
            )
    )
    @GetMapping
    public ResponseEntity<ResponseProjects> getProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ResponseProjects.from(projectQueryPortIn.getProjects(
                userDetails.getUserId(), cursor, size, status
        )));
    }

    @Operation(
            summary = "프로젝트 개요 조회",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "수집 건강 상태와 활동·개발로그·회고 집계 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseProjectOverview.class)
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
    @GetMapping("/{projectId}/overview")
    public ResponseEntity<ResponseProjectOverview> getOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(ResponseProjectOverview.from(
                projectQueryPortIn.getOverview(userDetails.getUserId(), projectId)
        ));
    }
}
