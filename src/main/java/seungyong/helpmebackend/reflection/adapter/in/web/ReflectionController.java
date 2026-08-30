package seungyong.helpmebackend.reflection.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.reflection.adapter.in.web.dto.request.RequestCreateReflection;
import seungyong.helpmebackend.reflection.adapter.in.web.dto.request.RequestRegenerateReflection;
import seungyong.helpmebackend.reflection.adapter.in.web.dto.request.RequestSaveReflection;
import seungyong.helpmebackend.reflection.adapter.in.web.dto.response.ResponseReflectionDetail;
import seungyong.helpmebackend.reflection.adapter.in.web.dto.response.ResponseReflectionGeneration;
import seungyong.helpmebackend.reflection.adapter.in.web.dto.response.ResponseReflections;
import seungyong.helpmebackend.reflection.adapter.in.web.dto.response.ResponseSavedReflection;
import seungyong.helpmebackend.reflection.application.port.in.ReflectionPortIn;
import seungyong.helpmebackend.reflection.application.port.in.command.ListReflectionsQuery;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionGenerationResult;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionErrorCode;

import java.net.URI;
import java.time.LocalDate;

@Tag(name = "Reflection", description = "프로젝트 일일·주간 회고 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/reflections")
@RequiredArgsConstructor
@UserRoleApiErrors
class ReflectionController {
    private final ReflectionPortIn reflectionPortIn;

    @Operation(summary = "회고 목록 조회")
    @GetMapping
    public ResponseEntity<ResponseReflections> getReflections(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestParam String kind,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ResponseReflections.from(
                reflectionPortIn.getReflections(new ListReflectionsQuery(
                        userDetails.getUserId(), projectId, kind,
                        from, to, status, cursor, size
                ))
        ));
    }

    @Operation(summary = "회고 상세 조회")
    @GetMapping("/{reflectionId}")
    public ResponseEntity<ResponseReflectionDetail> getReflection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long reflectionId
    ) {
        return ResponseEntity.ok(ResponseReflectionDetail.from(
                reflectionPortIn.getReflection(
                        userDetails.getUserId(), projectId, reflectionId
                )
        ));
    }

    @Operation(
            summary = "회고 생성",
            description = """
                    generationMode=ai는 작업을 durable queue에 저장한 뒤 202를 반환합니다.
                    이후 AI 호출 실패는 이 시작 응답을 바꾸지 않고 상세 조회의
                    status=failed, error.code로 확인합니다.
                    generationMode=blank 또는 필드 생략은 AI 호출 없이 편집 가능한
                    draft를 즉시 만듭니다.
                    동일 프로젝트·종류·기간 요청은 새 row를 만들지 않고 기존 회고를 반환합니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "422",
                    description = "생성 근거가 없거나 allowPartial=false인데 근거가 일부 누락되었습니다.",
                    errorCodeClasses = ReflectionErrorCode.class,
                    errorCodes = "REFLECTION_SOURCE_INSUFFICIENT"
            )
    })
    @PostMapping
    public ResponseEntity<ResponseReflectionGeneration> createReflection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody RequestCreateReflection request
    ) {
        ReflectionGenerationResult result = reflectionPortIn.createReflection(
                request.toCommand(userDetails.getUserId(), projectId)
        );
        return generationResponse(projectId, result);
    }

    @Operation(summary = "회고 저장")
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "409",
                    description = "조회한 version 이후 다른 변경이 먼저 저장되었습니다.",
                    errorCodeClasses = DocumentErrorCode.class,
                    errorCodes = "DOCUMENT_VERSION_CONFLICT"
            )
    })
    @PutMapping("/{reflectionId}")
    public ResponseEntity<ResponseSavedReflection> saveReflection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long reflectionId,
            @Valid @RequestBody RequestSaveReflection request
    ) {
        return ResponseEntity.ok(ResponseSavedReflection.from(
                reflectionPortIn.saveReflection(request.toCommand(
                        userDetails.getUserId(), projectId, reflectionId
                ))
        ));
    }

    @Operation(
            summary = "회고 재생성",
            description = """
                    기존 reflectionId를 유지하고 최신 DB 근거 snapshot으로 다시 queue합니다.
                    마지막 AI 성공 sourceHash와 최신 근거 hash가 같으면 AI를 호출하지 않고
                    기존 회고를 200으로 반환합니다.
                    AI 최종 실패는 HTTP 시작 오류가 아니라 상세 조회의 failed/error로 반환합니다.
                    queued 또는 generating 상태에서 반복 호출하면 작업을 중복 생성하지 않습니다.
                    """
    )
    @ApiErrorResponses({
            @ApiErrorResponse(
                    responseCode = "422",
                    description = "재생성할 근거가 없습니다.",
                    errorCodeClasses = ReflectionErrorCode.class,
                    errorCodes = "REFLECTION_SOURCE_INSUFFICIENT"
            )
    })
    @PostMapping("/{reflectionId}/regenerate")
    public ResponseEntity<ResponseReflectionGeneration> regenerateReflection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long reflectionId,
            @RequestBody(required = false) RequestRegenerateReflection request
    ) {
        RequestRegenerateReflection normalized = request == null
                ? new RequestRegenerateReflection(null) : request;
        ReflectionGenerationResult result = reflectionPortIn.regenerateReflection(
                normalized.toCommand(userDetails.getUserId(), projectId, reflectionId)
        );
        return generationResponse(projectId, result);
    }

    private ResponseEntity<ResponseReflectionGeneration> generationResponse(
            Long projectId, ReflectionGenerationResult result
    ) {
        if (result.asynchronous()) {
            return acceptedGenerationResponse(projectId, result);
        }
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(ResponseReflectionGeneration.from(projectId, result));
    }

    private ResponseEntity<ResponseReflectionGeneration> acceptedGenerationResponse(
            Long projectId, ReflectionGenerationResult result
    ) {
        ResponseReflectionGeneration response =
                ResponseReflectionGeneration.from(projectId, result);
        return ResponseEntity.accepted()
                .location(URI.create(response.location()))
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()))
                .body(response);
    }
}
