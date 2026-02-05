package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestReorder;
import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestSection;
import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestSectionContent;
import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.domain.exception.SectionErrorCode;
import seungyong.helpmebackend.domain.exception.UserErrorCode;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.section.SectionPortIn;

import java.net.URI;
import java.util.List;

@Tag(
        name = "Section",
        description = "Section 관련 API"
)
@RestController
@RequestMapping("/api/v1/repos/{owner}/{name}/sections")
@ResponseBody
@RequiredArgsConstructor
public class SectionController {
    private final SectionPortIn sectionPortIn;

    @Operation(
            summary = "Section 목록 조회",
            description = """
                    특정 Repository의 Section 목록을 조회합니다.
                    - 저장된 Section이 없는 경우 404 NOT FOUND 에러를 반환합니다.
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
                    description = "인증에 실패했습니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = { RepositoryErrorCode.class },
                    errorCodes = { "REPOSITORY_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { SectionErrorCode.class, UserErrorCode.class },
                    errorCodes = { "NOT_FOUND_SECTIONS", "NOT_FOUND_USER" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @GetMapping
    public ResponseEntity<ResponseSections> getSections(
            @PathVariable String owner,
            @PathVariable String name,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                sectionPortIn.getSections(details.getUserId(), owner, name)
        );
    }

    @Operation(
            summary = "Section 생성",
            description = """
                    특정 Repository에 Section을 생성합니다.
                    - title에 따라 '## title'로 content가 생성됩니다.
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
                    description = "인증에 실패했습니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = { RepositoryErrorCode.class },
                    errorCodes = { "REPOSITORY_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class },
                    errorCodes = { "NOT_FOUND_USER" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @PostMapping
    public ResponseEntity<ResponseSections.Section> createSection(
            @PathVariable String owner,
            @PathVariable String name,
            @Valid @RequestBody RequestSection request,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        ResponseSections.Section section = sectionPortIn.createSection(
                details.getUserId(),
                owner,
                name,
                request.title()
        );

        return ResponseEntity.created(URI.create("/api/v1/repos/" + owner + "/" + name + "/sections"))
                .body(section);
    }

    @Operation(
            summary = "Section 초기화",
            description = """
                    특정 Repository의 Section을 초기화합니다.
                    - branch: Section을 생성할 기준이 되는 브랜치 이름입니다.
                    - splitMode: Section 생성 모드입니다. "split" 또는 "whole" 값을 가질 수 있으며, 그 외의 값이 들어올 경우 "whole"로 처리됩니다.
                    - 이미 Section이 존재하는 경우 기존 Section을 모두 삭제하고 새로 생성합니다.
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
                    description = "인증에 실패했습니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = { RepositoryErrorCode.class },
                    errorCodes = { "REPOSITORY_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class },
                    errorCodes = { "NOT_FOUND_USER" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @PutMapping("/init")
    public ResponseEntity<ResponseSections> initSections(
            @PathVariable String owner,
            @PathVariable String name,
            @RequestParam String branch,
            @RequestParam String splitMode,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        ResponseSections response = sectionPortIn.initSections(details.getUserId(), owner, name, branch, splitMode);

        return ResponseEntity.created(URI.create("/api/v1/repos/" + owner + "/" + name + "/sections"))
                .body(response);
    }

    @Operation(
            summary = "Section 내용 수정",
            description = """
                    특정 Repository의 Section 내용을 수정합니다.
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
                    description = "인증에 실패했습니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = { RepositoryErrorCode.class },
                    errorCodes = { "REPOSITORY_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "NOT_FOUND_USER", "NOT_FOUND_SECTIONS" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @PutMapping("/content")
    public ResponseEntity<Void> updateSectionContent(
            @PathVariable String owner,
            @PathVariable String name,
            @RequestBody RequestSectionContent request,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        sectionPortIn.updateSectionContent(details.getUserId(), owner, name, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Section 순서 변경",
            description = """
                    특정 Repository의 Section 순서를 변경합니다.
                    - request.ids에 들어있는 Section ID 순서대로 Section의 순서가 변경됩니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "잘못된 요청입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, SectionErrorCode.class },
                    errorCodes = { "BAD_REQUEST", "INVALID_REORDER_REQUEST" }
            ),
            @ApiErrorResponse(
                    responseCode = "401",
                    description = "인증에 실패했습니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = { RepositoryErrorCode.class },
                    errorCodes = { "REPOSITORY_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "NOT_FOUND_USER", "NOT_FOUND_SECTIONS" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderSection(
            @PathVariable String owner,
            @PathVariable String name,
            @RequestBody RequestReorder request,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        sectionPortIn.reorderSections(details.getUserId(), owner, name, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Section 삭제",
            description = """
                    특정 Section을 삭제합니다.
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
                    description = "인증에 실패했습니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = { RepositoryErrorCode.class },
                    errorCodes = { "REPOSITORY_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, SectionErrorCode.class },
                    errorCodes = { "NOT_FOUND_USER", "NOT_FOUND_SECTIONS" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class },
                    errorCodes = { "INTERNAL_SERVER_ERROR" }
            )
    })
    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(
            @PathVariable String owner,
            @PathVariable String name,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        sectionPortIn.deleteSection(details.getUserId(), owner, name, sectionId);
        return ResponseEntity.noContent().build();

    }
}
