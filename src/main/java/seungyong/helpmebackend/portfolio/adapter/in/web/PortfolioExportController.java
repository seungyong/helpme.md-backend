package seungyong.helpmebackend.portfolio.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.request.RequestPortfolioConflictResolution;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.request.RequestStartPortfolioExport;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioDownload;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioExport;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioExportAccepted;
import seungyong.helpmebackend.portfolio.adapter.in.web.dto.response.ResponsePortfolioExports;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioExportPortIn;
import seungyong.helpmebackend.portfolio.application.port.in.command.ListPortfolioExportsQuery;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Tag(name = "Portfolio Export", description = "PDF·Notion 포트폴리오 내보내기 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/portfolios/{portfolioId}/exports")
@RequiredArgsConstructor
@UserRoleApiErrors
class PortfolioExportController {
    private final PortfolioExportPortIn portIn;

    @Operation(summary = "내보내기 목록 조회")
    @GetMapping
    ResponseEntity<ResponsePortfolioExports> getExports(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long projectId,
            @PathVariable Long portfolioId, @RequestParam(required = false) String format,
            @RequestParam(required = false) String status, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ResponsePortfolioExports.from(portIn.getExports(
                new ListPortfolioExportsQuery(user.getUserId(), projectId, portfolioId,
                        parseFormat(format), parseStatus(status), cursor, size))));
    }

    @Operation(summary = "내보내기 상태 조회", description = "비동기 실패·충돌·만료도 HTTP 200 상태 본문으로 반환합니다.")
    @GetMapping("/{exportId}")
    ResponseEntity<ResponsePortfolioExport> getExport(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long projectId,
            @PathVariable Long portfolioId, @PathVariable Long exportId) {
        return ResponseEntity.ok(ResponsePortfolioExport.from(
                portIn.getExport(user.getUserId(), projectId, portfolioId, exportId),
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Operation(summary = "내보내기 시작", description = "요청한 portfolioVersion 문서를 고정하고 202를 반환합니다.")
    @PostMapping
    ResponseEntity<ResponsePortfolioExportAccepted> start(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long projectId,
            @PathVariable Long portfolioId, @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody RequestStartPortfolioExport request) {
        PortfolioExport export = portIn.startExport(
                request.toCommand(user.getUserId(), projectId, portfolioId, idempotencyKey));
        return accepted(projectId, portfolioId, export);
    }

    @Operation(summary = "PDF 다운로드 URL 발급")
    @GetMapping("/{exportId}/download")
    ResponseEntity<ResponsePortfolioDownload> download(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long projectId,
            @PathVariable Long portfolioId, @PathVariable Long exportId) {
        return ResponseEntity.ok(ResponsePortfolioDownload.from(
                portIn.getDownload(user.getUserId(), projectId, portfolioId, exportId)));
    }

    @Operation(summary = "실패한 내보내기 재시도", description = "현재 문서가 아닌 기존 고정 snapshot을 재사용합니다.")
    @PostMapping("/{exportId}/retry")
    ResponseEntity<ResponsePortfolioExportAccepted> retry(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long projectId,
            @PathVariable Long portfolioId, @PathVariable Long exportId) {
        return accepted(projectId, portfolioId,
                portIn.retryExport(user.getUserId(), projectId, portfolioId, exportId));
    }

    @Operation(summary = "Notion 제목 충돌 해결", description = "같은 exportId를 processing 흐름으로 다시 진행합니다.")
    @PostMapping("/{exportId}/conflict-resolution")
    ResponseEntity<ResponsePortfolioExportAccepted> resolve(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long projectId,
            @PathVariable Long portfolioId, @PathVariable Long exportId,
            @Valid @RequestBody RequestPortfolioConflictResolution request) {
        return accepted(projectId, portfolioId, portIn.resolveConflict(
                user.getUserId(), projectId, portfolioId, exportId, request.toAction()));
    }

    private ResponseEntity<ResponsePortfolioExportAccepted> accepted(
            Long projectId, Long portfolioId, PortfolioExport export) {
        ResponsePortfolioExportAccepted response = ResponsePortfolioExportAccepted.from(projectId, portfolioId, export);
        return ResponseEntity.accepted().location(URI.create(response.location()))
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(response.retryAfterSeconds())).body(response);
    }

    private PortfolioExportFormat parseFormat(String format) {
        if (format == null) return null;
        try {
            return PortfolioExportFormat.fromDatabaseValue(format);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private PortfolioExportStatus parseStatus(String status) {
        if (status == null) return null;
        try {
            return PortfolioExportStatus.fromDatabaseValue(status);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }
}
