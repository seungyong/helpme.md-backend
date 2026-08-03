package seungyong.helpmebackend.github.adapter.in.web;

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
import seungyong.helpmebackend.github.adapter.in.web.dto.response.ResponseGithubInstallations;
import seungyong.helpmebackend.github.adapter.in.web.dto.response.ResponseGithubRepositories;
import seungyong.helpmebackend.github.application.port.in.GithubAppPortIn;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;

@Tag(name = "GitHub App", description = "GitHub App 설치와 Repository 탐색 API")
@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@UserRoleApiErrors
class GithubAppController {
    private final GithubAppPortIn githubAppPortIn;

    @Operation(
            summary = "GitHub 설치 계정 조회",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "설치 계정 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseGithubInstallations.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "GitHub 연결이 회수되었습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_CONNECTION_REVOKED"
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
    @GetMapping("/installations")
    public ResponseEntity<ResponseGithubInstallations> getInstallations(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ResponseGithubInstallations.from(
                githubAppPortIn.getInstallations(userDetails.getUserId())
        ));
    }

    @Operation(
            summary = "GitHub 설치 Repository 조회",
            description = """
                    q가 없으면 GitHub pagination으로 요청한 한 페이지만 조회합니다.
                    q가 있으면 해당 installation에 허용된 Repository만 전체 조회한 뒤 fullName을 검색합니다.
                    Branch 목록은 포함하지 않으며 Repository 선택 후 기존 /repos/{owner}/{name}/branches API로 조회합니다.
                    cursor는 서버가 반환한 값을 해석하지 않고 그대로 재전송해야 합니다.
                    """,
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "설치 Repository 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseGithubRepositories.class)
                    )
            )
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "installationId, cursor 또는 size가 올바르지 않습니다.",
                    errorCodeClasses = GlobalErrorCode.class,
                    errorCodes = "BAD_REQUEST"
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "GitHub 연결이 회수되었거나 Repository 권한이 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = { "GITHUB_CONNECTION_REVOKED", "GITHUB_PERMISSION_DENIED" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "installation 또는 Repository를 찾을 수 없습니다.",
                    errorCodeClasses = GithubErrorCode.class,
                    errorCodes = "GITHUB_RESOURCE_NOT_FOUND"
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
    @GetMapping("/installations/{installationId}/repositories")
    public ResponseEntity<ResponseGithubRepositories> getRepositories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long installationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "30") Integer size
    ) {
        return ResponseEntity.ok(ResponseGithubRepositories.from(
                githubAppPortIn.getRepositories(
                        userDetails.getUserId(),
                        installationId,
                        q,
                        cursor,
                        size
                )
        ));
    }
}
