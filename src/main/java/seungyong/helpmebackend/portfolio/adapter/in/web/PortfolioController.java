package seungyong.helpmebackend.portfolio.adapter.in.web;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.request.RequestCreatePortfolio;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.request.RequestRegeneratePortfolio;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.request.RequestSavePortfolio;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioDetail;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioGeneration;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioSources;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolios;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponseSavedPortfolio;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioRegeneration;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioPortIn;
import seungyong.helpmebackend.portfolio.application.port.in.command.GetPortfolioSourcesQuery;
import seungyong.helpmebackend.portfolio.application.port.in.command.ListPortfoliosQuery;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioGenerationResult;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Portfolio", description = "회고 기반 포트폴리오 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
@UserRoleApiErrors
class PortfolioController {
    private final PortfolioPortIn portfolioPortIn;

    @Operation(summary = "포트폴리오 생성 근거 조회")
    @GetMapping("/portfolio-sources")
    public ResponseEntity<ResponsePortfolioSources> getSources(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {
        return ResponseEntity.ok(ResponsePortfolioSources.from(portfolioPortIn.getSources(
                new GetPortfolioSourcesQuery(userDetails.getUserId(), projectId, periodStart, periodEnd)
        )));
    }

    @Operation(summary = "포트폴리오 목록 조회")
    @GetMapping("/portfolios")
    public ResponseEntity<ResponsePortfolios> getPortfolios(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ResponsePortfolios.from(portfolioPortIn.getPortfolios(
                new ListPortfoliosQuery(userDetails.getUserId(), projectId, status, cursor, size)
        )));
    }

    @Operation(summary = "포트폴리오 상세 및 생성 상태 조회")
    @ApiErrorResponses({
            @ApiErrorResponse(responseCode = "404", description = "포트폴리오를 찾을 수 없음",
                    errorCodeClasses = PortfolioErrorCode.class, errorCodes = "PORTFOLIO_NOT_FOUND")
    })
    @GetMapping("/portfolios/{portfolioId}")
    public ResponseEntity<ResponsePortfolioDetail> getPortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long portfolioId
    ) {
        return ResponseEntity.ok(ResponsePortfolioDetail.from(
                portfolioPortIn.getPortfolio(userDetails.getUserId(), projectId, portfolioId)
        ));
    }

    @Operation(summary = "포트폴리오 생성", description = """
            ai 모드는 snapshot을 durable queue에 저장한 뒤 202를 반환합니다.
            동일 Idempotency-Key 재요청은 같은 portfolioId를 반환합니다.
            202 이후 최종 AI 실패는 상세 GET 200의 status=failed와 error로 확인합니다.
            blank 모드는 AI 호출 없이 201 draft를 반환합니다.
            """)
    @ApiErrorResponses({
            @ApiErrorResponse(responseCode = "422", description = "저장 회고 누락 또는 비공개 근거 선택",
                    errorCodeClasses = PortfolioErrorCode.class,
                    errorCodes = {"PORTFOLIO_SOURCE_REQUIRED", "PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED"})
    })
    @PostMapping("/portfolios")
    public ResponseEntity<ResponsePortfolioGeneration> createPortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody RequestCreatePortfolio request
    ) {
        PortfolioGenerationResult result = portfolioPortIn.createPortfolio(
                request.toCommand(userDetails.getUserId(), projectId, idempotencyKey)
        );
        return generationResponse(projectId, result);
    }

    @Operation(summary = "포트폴리오 저장")
    @ApiErrorResponses({
            @ApiErrorResponse(responseCode = "404", description = "포트폴리오를 찾을 수 없음",
                    errorCodeClasses = PortfolioErrorCode.class, errorCodes = "PORTFOLIO_NOT_FOUND"),
            @ApiErrorResponse(responseCode = "409", description = "조회 이후 다른 변경이 먼저 저장됨",
                    errorCodeClasses = DocumentErrorCode.class, errorCodes = "DOCUMENT_VERSION_CONFLICT"),
            @ApiErrorResponse(responseCode = "422", description = "비공개 또는 서명 근거가 문서에 포함됨",
                    errorCodeClasses = PortfolioErrorCode.class,
                    errorCodes = "PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED")
    })
    @PutMapping("/portfolios/{portfolioId}")
    public ResponseEntity<ResponseSavedPortfolio> savePortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long portfolioId,
            @Valid @RequestBody RequestSavePortfolio request
    ) {
        return ResponseEntity.ok(ResponseSavedPortfolio.from(portfolioPortIn.savePortfolio(
                request.toCommand(userDetails.getUserId(), projectId, portfolioId)
        )));
    }

    @Operation(summary = "포트폴리오 재생성", description = """
            같은 portfolioId를 queued로 전환합니다. refreshSources=false는 기존 snapshot을 유지하고,
            true는 선택된 회고의 현재 version과 공개 근거를 다시 검증해 snapshot을 교체합니다.
            AI 실패 시 이전 content와 version은 유지됩니다.
            """)
    @ApiErrorResponses({
            @ApiErrorResponse(responseCode = "404", description = "포트폴리오를 찾을 수 없음",
                    errorCodeClasses = PortfolioErrorCode.class, errorCodes = "PORTFOLIO_NOT_FOUND"),
            @ApiErrorResponse(responseCode = "422", description = "갱신할 saved 회고 근거가 없음",
                    errorCodeClasses = PortfolioErrorCode.class, errorCodes = "PORTFOLIO_SOURCE_REQUIRED")
    })
    @PostMapping("/portfolios/{portfolioId}/regenerate")
    public ResponseEntity<ResponsePortfolioRegeneration> regeneratePortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long portfolioId,
            @RequestBody(required = false) RequestRegeneratePortfolio request
    ) {
        RequestRegeneratePortfolio normalized = request == null
                ? new RequestRegeneratePortfolio(false) : request;
        PortfolioGenerationResult result = portfolioPortIn.regeneratePortfolio(
                normalized.toCommand(userDetails.getUserId(), projectId, portfolioId)
        );
        ResponsePortfolioRegeneration response = ResponsePortfolioRegeneration.from(projectId, result);
        return ResponseEntity.accepted().location(URI.create(response.location()))
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(response.retryAfterSeconds())).body(response);
    }

    private ResponseEntity<ResponsePortfolioGeneration> generationResponse(
            Long projectId, PortfolioGenerationResult result
    ) {
        ResponsePortfolioGeneration response = ResponsePortfolioGeneration.from(projectId, result);
        if (result.asynchronous()) {
            return ResponseEntity.accepted().location(URI.create(response.location()))
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds())).body(response);
        }
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }
}
