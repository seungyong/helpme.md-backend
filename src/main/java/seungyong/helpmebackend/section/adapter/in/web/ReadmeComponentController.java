package seungyong.helpmebackend.section.adapter.in.web;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.CreateReadmeComponentRequest;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.DeleteReadmeComponentRequest;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.UpdateReadmeComponentRequest;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ReadmeComponentResponse;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ReadmeComponentsResponse;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.UpdatedReadmeComponentResponse;
import seungyong.helpmebackend.section.application.port.in.ReadmeComponentPortIn;
import seungyong.helpmebackend.section.application.port.in.command.CreateReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.DeleteReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.UpdateReadmeComponentCommand;
import seungyong.helpmebackend.section.domain.exception.SectionErrorCode;

import java.net.URI;

@Tag(name = "README Component", description = "README 컴포넌트 조회·편집 API")
@RestController
@RequestMapping("/api/v1/repos/{owner}/{name}/components")
@RequiredArgsConstructor
@UserRoleApiErrors
class ReadmeComponentController {
    private final ReadmeComponentPortIn readmeComponentPortIn;

    @Operation(
            summary = "컴포넌트 목록 조회",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "orderIdx 순서의 컴포넌트 목록. 저장된 항목이 없으면 빈 배열입니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReadmeComponentsResponse.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "GitHub 연결 또는 Repository 쓰기 권한이 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = {"GITHUB_CONNECTION_REVOKED", "GITHUB_PERMISSION_DENIED"}
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "연결된 GitHub Repository를 찾을 수 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RESOURCE_NOT_FOUND"
            ),
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "프로젝트가 활성 상태가 아닙니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_NOT_ACTIVE"
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "GitHub API 요청 한도를 초과했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RATE_LIMIT_EXCEEDED"
            ),
            @ApiErrorResponse(
                    responseCode = "502",
                    description = "GitHub API가 비정상 응답을 반환했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_UPSTREAM_ERROR"
            )
    })
    @GetMapping
    public ResponseEntity<ReadmeComponentsResponse> getComponents(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String owner,
            @PathVariable String name
    ) {
        return ResponseEntity.ok(ReadmeComponentsResponse.from(
                readmeComponentPortIn.getComponents(
                        userDetails.getUserId(), owner, name
                )
        ));
    }

    @Operation(
            summary = "컴포넌트 추가",
            description = "orderIdx가 없으면 마지막에 추가하고, content가 없으면 빈 문자열로 저장합니다.",
            responses = @ApiResponse(
                    responseCode = "201",
                    description = "컴포넌트 추가 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReadmeComponentResponse.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "요청 형식 또는 orderIdx 범위가 올바르지 않습니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = "BAD_REQUEST"
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "GitHub 연결 또는 Repository 쓰기 권한이 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = {"GITHUB_CONNECTION_REVOKED", "GITHUB_PERMISSION_DENIED"}
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "연결된 GitHub Repository를 찾을 수 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RESOURCE_NOT_FOUND"
            ),
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "프로젝트가 활성 상태가 아닙니다.",
                    errorCodeClasses = ProjectErrorCode.class,
                    errorCodes = "PROJECT_NOT_ACTIVE"
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "GitHub API 요청 한도를 초과했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RATE_LIMIT_EXCEEDED"
            ),
            @ApiErrorResponse(
                    responseCode = "502",
                    description = "GitHub API가 비정상 응답을 반환했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_UPSTREAM_ERROR"
            )
    })
    @PostMapping
    public ResponseEntity<ReadmeComponentResponse> createComponent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String owner,
            @PathVariable String name,
            @Valid @RequestBody CreateReadmeComponentRequest request
    ) {
        ReadmeComponentResponse response = ReadmeComponentResponse.from(
                readmeComponentPortIn.createComponent(
                        new CreateReadmeComponentCommand(
                                userDetails.getUserId(),
                                owner,
                                name,
                                request.title(),
                                request.content(),
                                request.orderIdx()
                        )
                )
        );
        return ResponseEntity.created(URI.create(
                "/api/v1/repos/%s/%s/components/%d".formatted(
                        owner, name, response.id()
                )
        )).body(response);
    }

    @Operation(
            summary = "컴포넌트 수정",
            description = "목록 응답의 현재 version을 보내야 하며, 성공하면 version이 1 증가합니다.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "컴포넌트 수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatedReadmeComponentResponse.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "요청 형식 또는 orderIdx/version 범위가 올바르지 않습니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = "BAD_REQUEST"
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "GitHub 연결 또는 Repository 쓰기 권한이 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = {"GITHUB_CONNECTION_REVOKED", "GITHUB_PERMISSION_DENIED"}
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "Repository 또는 컴포넌트를 찾을 수 없습니다.",
                    errorCodeClasses = {GithubErrorCode.class, SectionErrorCode.class},
                    errorCodes = {"GITHUB_RESOURCE_NOT_FOUND", "NOT_FOUND_SECTIONS"}
            ),
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "프로젝트 상태 또는 문서 version이 충돌했습니다.",
                    errorCodeClasses = {ProjectErrorCode.class, DocumentErrorCode.class},
                    errorCodes = {"PROJECT_NOT_ACTIVE", "DOCUMENT_VERSION_CONFLICT"}
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "GitHub API 요청 한도를 초과했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RATE_LIMIT_EXCEEDED"
            ),
            @ApiErrorResponse(
                    responseCode = "502",
                    description = "GitHub API가 비정상 응답을 반환했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_UPSTREAM_ERROR"
            )
    })
    @PatchMapping("/{componentId}")
    public ResponseEntity<UpdatedReadmeComponentResponse> updateComponent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String owner,
            @PathVariable String name,
            @PathVariable Long componentId,
            @Valid @RequestBody UpdateReadmeComponentRequest request
    ) {
        return ResponseEntity.ok(UpdatedReadmeComponentResponse.from(
                readmeComponentPortIn.updateComponent(
                        new UpdateReadmeComponentCommand(
                                userDetails.getUserId(),
                                owner,
                                name,
                                componentId,
                                request.title(),
                                request.content(),
                                request.orderIdx(),
                                request.version()
                        )
                )
        ));
    }

    @Operation(
            summary = "컴포넌트 삭제",
            description = "삭제와 남은 컴포넌트의 orderIdx 재정렬을 같은 트랜잭션에서 처리합니다.",
            responses = @ApiResponse(responseCode = "204", description = "컴포넌트 삭제 성공")
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
                    description = "GitHub 연결 또는 Repository 쓰기 권한이 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = {"GITHUB_CONNECTION_REVOKED", "GITHUB_PERMISSION_DENIED"}
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "Repository 또는 컴포넌트를 찾을 수 없습니다.",
                    errorCodeClasses = {GithubErrorCode.class, SectionErrorCode.class},
                    errorCodes = {"GITHUB_RESOURCE_NOT_FOUND", "NOT_FOUND_SECTIONS"}
            ),
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "프로젝트 상태 또는 문서 version이 충돌했습니다.",
                    errorCodeClasses = {ProjectErrorCode.class, DocumentErrorCode.class},
                    errorCodes = {"PROJECT_NOT_ACTIVE", "DOCUMENT_VERSION_CONFLICT"}
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "GitHub API 요청 한도를 초과했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RATE_LIMIT_EXCEEDED"
            ),
            @ApiErrorResponse(
                    responseCode = "502",
                    description = "GitHub API가 비정상 응답을 반환했습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_UPSTREAM_ERROR"
            )
    })
    @DeleteMapping("/{componentId}")
    public ResponseEntity<Void> deleteComponent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String owner,
            @PathVariable String name,
            @PathVariable Long componentId,
            @Valid @RequestBody DeleteReadmeComponentRequest request
    ) {
        readmeComponentPortIn.deleteComponent(new DeleteReadmeComponentCommand(
                userDetails.getUserId(),
                owner,
                name,
                componentId,
                request.version()
        ));
        return ResponseEntity.noContent().build();
    }
}
