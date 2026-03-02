package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestPull;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.*;
import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.domain.exception.UserErrorCode;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.repository.RepositoryPortIn;

@Tag(
        name = "Repo",
        description = """
                Repo 관련 API
                - 모든 Repo에 대한 정보는 Redis에 저장되어, `3일` 후 만료됩니다.
                - 처음 요청 시점에서는 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                - 이후 동일한 요청(최신 커밋 기준)에 대해서는 Redis 캐시에서 데이터를 반환합니다.
                """
)
@RestController
@RequestMapping("/api/v1/repos")
@ResponseBody
@RequiredArgsConstructor
public class RepoController {
    private final RepositoryPortIn repositoryPortIn;

    @Operation(
            summary = "레포지토리 목록 조회",
            description = """
                    Github App이 설치된 특정 계정의 레포지토리 목록을 조회합니다.
                    
                    - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                    - DB에 레포지토리 정보가 저장되지 않습니다.
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
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "GITHUB_UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "USER_NOT_FOUND", "INSTALLED_REPOSITORY_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "요청 한도를 초과했습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_RATE_LIMIT_EXCEEDED" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "JSON_PROCESSING_ERROR", "GITHUB_ERROR", "INTERNAL_SERVER_ERROR" }
            )
    })
    @GetMapping
    public ResponseEntity<ResponseRepositories> getRepositories(
            @RequestParam("installation_id") Long installationId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "per_page", defaultValue = "30") Integer perPage,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.getRepositories(details.getUserId(), installationId, page, perPage)
        );
    }

    @Operation(
            summary = "레포지토리 상세 조회",
            description = """
                    특정 레포지토리의 상세 정보를 조회합니다.
                    
                    - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                    - DB에 레포지토리 정보가 저장되지 않습니다.
                    - Readme.md 파일이 없는 경우 빈 내용을 반환합니다.
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
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "GITHUB_UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_FORBIDDEN", "REPOSITORY_CANNOT_PULL" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class },
                    errorCodes = { "USER_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "요청 한도를 초과했습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_RATE_LIMIT_EXCEEDED", "GITHUB_BRANCHES_TOO_MANY_REQUESTS" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "JSON_PROCESSING_ERROR", "GITHUB_ERROR", "INTERNAL_SERVER_ERROR" }
            )
    })
    @GetMapping("/{owner}/{name}")
    public ResponseEntity<ResponseRepository> getRepository(
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.getRepository(details.getUserId(), owner, name)
        );
    }

    @Operation(
            summary = "레포지토리 브랜치 목록 조회",
            description = """
                    특정 레포지토리의 브랜치 목록을 조회합니다.
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
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "GITHUB_UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "USER_NOT_FOUND", "REPOSITORY_OR_BRANCH_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "요청 한도를 초과했습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_RATE_LIMIT_EXCEEDED", "GITHUB_BRANCHES_TOO_MANY_REQUESTS" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "JSON_PROCESSING_ERROR", "GITHUB_ERROR", "INTERNAL_SERVER_ERROR" }
            )
    })
    @GetMapping("/{owner}/{name}/branches")
    public ResponseEntity<ResponseBranches> getBranches(
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.getBranches(details.getUserId(), owner, name)
        );
    }

    @Operation(
            summary = "임시 저장된 README 초안 평가 결과 조회",
            description = """
                    SSE 작업 중 에러가 발생한 경우, 임시 저장된 README 초안 평가 결과를 조회합니다.
                    """
    )
    @GetMapping("/fallback/evaluate/draft/{taskId}")
    public ResponseEntity<ResponseEvaluation> getFallbackDraftEvaluation(
            @PathVariable("taskId") String taskId
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.fallbackDraftEvaluation(taskId)
        );
    }

    @Operation(
            summary = "임시 저장된 README 내용 조회",
            description = """
                    SSE 작업 중 에러가 발생한 경우, 임시 저장된 README 평가 또는 생성 결과를 조회합니다.
                    """
    )
    @GetMapping("/fallback/generate/{taskId}")
    public ResponseEntity<ResponseSections> getFallbackGenerate(
            @PathVariable("taskId") String taskId
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.fallbackGenerateReadme(taskId)
        );
    }

    @Operation(
            summary = "풀 리퀘스트 생성",
            description = """
                    특정 레포지토리에 대해 특정 브랜치를 기준으로 풀 리퀘스트를 생성합니다.
                    
                    - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                    - 특정 브랜치의 최신 커밋을 기준으로 `readme-proposals/{UUID}` 브랜치를 생성하고, 해당 브랜치를 기준으로 풀 리퀘스트를 생성합니다.
                    - 사용자는 직접 PR을 검토하고, 머지를 해야만 합니다.
                    
                    PR 플로우는 다음과 같습니다:
                    1. 사용자가 `main` 브랜치를 기준으로 풀 리퀘스트 생성 요청
                    2. 시스템이 `main` 브랜치의 최신 커밋을 기준으로 `readme-proposals/{UUID}` 브랜치를 생성
                    3. 시스템이 `readme-proposals/{UUID}` 브랜치를 기준으로 README.md 수정 내용을 Push
                    4. 시스템이 `readme-proposals/{UUID}` 브랜치를 기준으로 `main` 브랜치에 대한 풀 리퀘스트 생성
                    5. 만약, 중간에 에러가 발생하는 경우 생성된 브랜치 삭제
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "400",
                    description = "잘못된 요청입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "BAD_REQUEST", "BAD_REQUEST_SAME_BRANCH" }
            ),
            @ApiErrorResponse(
                    responseCode = "401",
                    description = "인증에 실패했습니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "GITHUB_UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "USER_NOT_FOUND", "BRANCH_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "요청 한도를 초과했습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_RATE_LIMIT_EXCEEDED" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "JSON_PROCESSING_ERROR", "GITHUB_ERROR", "INTERNAL_SERVER_ERROR" }
            )
    })
    @PostMapping("/{owner}/{name}")
    public ResponseEntity<ResponsePull> createPullRequest(
            @Valid @RequestBody RequestPull request,
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.createPullRequest(request, details.getUserId(), owner, name)
        );
    }

    @Operation(
            summary = "README 평가",
            description = """
                    특정 레포지토리에서 사용자가 작성한 README.md를 평가합니다.
                    
                    - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                    - AI 모델을 사용하여 README.md 초안의 품질을 평가하고, 개선점을 제안합니다.
                    - 커밋 내역, 프로젝트 구조, 주요 파일 내용 및 목록, 언어 통계 등을 Redis 캐시에 저장하여 평가에 활용합니다.
                        - 가장 최신 커밋 SHA를 활용하여, 동일한 커밋에 대해 중복 평가를 방지합니다.
                    - SSE를 사용하며, 비동기 작업을 진행합니다.
                    - 평가 결과는 DB에 저장되지 않습니다.
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
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "GITHUB_UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "USER_NOT_FOUND", "REPOSITORY_OR_BRANCH_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "요청 한도를 초과했습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_RATE_LIMIT_EXCEEDED" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "JSON_PROCESSING_ERROR", "GITHUB_ERROR", "INTERNAL_SERVER_ERROR" }
            )
    })
    @PostMapping("/{owner}/{name}/evaluate/draft/sse")
    public ResponseEntity<Void> evaluateDraftReadme(
            @Valid @RequestBody RequestDraftEvaluation request,
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @RequestParam("taskId") String taskId,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        repositoryPortIn.evaluateDraftReadme(request, taskId, details.getUserId(), owner, name);
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "README 초안 생성",
            description = """
                    특정 레포지토리에 대해 README.md 초안을 생성합니다.
                    
                    - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                    - AI 모델을 사용하여 레포지토리의 특성에 맞는 README.md 초안을 생성합니다.
                    - SSE를 사용하며, 비동기 작업을 진행합니다.
                    - 커밋 내역, 프로젝트 구조, 주요 파일 내용 및 목록, 언어 통계 등을 Redis 캐시에 저장하여 생성에 활용합니다.
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
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "GITHUB_UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "403",
                    description = "권한이 없습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_FORBIDDEN" }
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClasses = { UserErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "USER_NOT_FOUND", "REPOSITORY_OR_BRANCH_NOT_FOUND" }
            ),
            @ApiErrorResponse(
                    responseCode = "429",
                    description = "요청 한도를 초과했습니다.",
                    errorCodeClasses = RepositoryErrorCode.class,
                    errorCodes = { "GITHUB_RATE_LIMIT_EXCEEDED" }
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClasses = { GlobalErrorCode.class, RepositoryErrorCode.class },
                    errorCodes = { "JSON_PROCESSING_ERROR", "GITHUB_ERROR", "INTERNAL_SERVER_ERROR" }
            )
    })
    @PostMapping("/{owner}/{name}/generate/sse")
    public ResponseEntity<Void> generateDraftReadme(
            @Valid @RequestBody RequestEvaluation request,
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @RequestParam("taskId") String taskId,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        repositoryPortIn.generateDraftReadme(request, taskId, details.getUserId(), owner, name);
        return ResponseEntity.accepted().build();
    }
}
