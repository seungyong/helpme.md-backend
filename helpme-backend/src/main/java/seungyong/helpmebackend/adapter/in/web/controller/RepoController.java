package seungyong.helpmebackend.adapter.in.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseDraftReadme;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepositories;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.exception.UserErrorCode;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.usecase.port.in.repository.RepositoryPortIn;

@Tag(
        name = "Repo",
        description = """
                Repo 관련 API
                - 모든 Repo에 대한 정보는 DB에 **저장되지 않습니다.**
                - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
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
                    errorCodeClass = GlobalErrorCode.class,
                    errorCodes = {"BAD_REQUEST"}
            ),
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClass = UserErrorCode.class,
                    errorCodes = {"USER_NOT_FOUND"}
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClass = GlobalErrorCode.class,
                    errorCodes = {"INTERNAL_SERVER_ERROR", "GITHUB_ERROR"}
            )
    })
    @GetMapping
    public ResponseEntity<ResponseRepositories> getRepositories(
            @RequestParam("installationId") Long installationId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.getRepositories(details.getUserId(), installationId, page)
        );
    }

    @Operation(
            summary = "레포지토리 상세 조회",
            description = """
                    특정 레포지토리의 상세 정보를 조회합니다.
                    
                    - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                    - DB에 레포지토리 정보가 저장되지 않습니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없습니다.",
                    errorCodeClass = UserErrorCode.class,
                    errorCodes = {"USER_NOT_FOUND"}
            ),
            @ApiErrorResponse(
                    responseCode = "500",
                    description = "서버 에러입니다.",
                    errorCodeClass = GlobalErrorCode.class,
                    errorCodes = {"INTERNAL_SERVER_ERROR", "GITHUB_ERROR"}
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

    @PostMapping("/{owner}/{name}/evaluate/readme")
    public ResponseEntity<ResponseEvaluation> evaluateReadme(
            @Valid @RequestBody RequestEvaluation request,
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @AuthenticationPrincipal CustomUserDetails details
            ) throws JsonProcessingException {
        return ResponseEntity.ok(
                repositoryPortIn.evaluateReadme(request, details.getUserId(), owner, name)
        );
    }

    @PostMapping("/{owner}/{name}/evaluate/draft")
    public ResponseEntity<ResponseEvaluation> evaluateDraftReadme(
            @Valid @RequestBody RequestDraftEvaluation request,
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @AuthenticationPrincipal CustomUserDetails details
    ) throws JsonProcessingException {
        return ResponseEntity.ok(
                repositoryPortIn.evaluateDraftReadme(request, details.getUserId(), owner, name)
        );
    }

    @PostMapping("/{owner}/{name}/generate")
    public ResponseEntity<ResponseDraftReadme> generateDraftReadme(
            @Valid @RequestBody RequestEvaluation request,
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @AuthenticationPrincipal CustomUserDetails details
    ) throws JsonProcessingException {
        return ResponseEntity.ok(
                repositoryPortIn.generateDraftReadme(request, details.getUserId(), owner, name)
        );
    }
}
