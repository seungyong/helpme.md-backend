package seungyong.helpmebackend.notion.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.notion.adapter.in.web.dto.request.RequestNotionAuthorization;
import seungyong.helpmebackend.notion.adapter.in.web.dto.request.RequestNotionDefaultPage;
import seungyong.helpmebackend.notion.adapter.in.web.dto.response.ResponseNotionAuthorization;
import seungyong.helpmebackend.notion.adapter.in.web.dto.response.ResponseNotionConnection;
import seungyong.helpmebackend.notion.adapter.in.web.dto.response.ResponseNotionDefaultPage;
import seungyong.helpmebackend.notion.adapter.in.web.dto.response.ResponseNotionPages;
import seungyong.helpmebackend.notion.application.port.in.NotionPortIn;
import seungyong.helpmebackend.notion.application.port.in.command.StartNotionAuthorizationCommand;
import seungyong.helpmebackend.notion.application.port.in.command.UpdateNotionDefaultPageCommand;

@Tag(name = "Notion", description = "Notion 연결 및 기본 페이지 API")
@RestController
@RequestMapping("/api/v1/integrations/notion")
@RequiredArgsConstructor
@UserRoleApiErrors
class NotionController {
    private final NotionPortIn notionPortIn;

    @Operation(summary = "Notion OAuth 연결 시작")
    @PostMapping("/oauth/authorize")
    ResponseEntity<ResponseNotionAuthorization> authorize(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) RequestNotionAuthorization request
    ) {
        String returnUrl = request == null ? null : request.returnUrl();
        return ResponseEntity.ok(ResponseNotionAuthorization.from(
                notionPortIn.startAuthorization(new StartNotionAuthorizationCommand(
                        userDetails.getUserId(), returnUrl
                ))
        ));
    }

    @Operation(summary = "Notion 연결 상태 조회")
    @GetMapping
    ResponseEntity<ResponseNotionConnection> getConnection(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ResponseNotionConnection.from(
                notionPortIn.getConnection(userDetails.getUserId())
        ));
    }

    @Operation(summary = "Notion 페이지 탐색")
    @GetMapping("/pages")
    ResponseEntity<ResponseNotionPages> getPages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ResponseNotionPages.from(
                notionPortIn.getPages(userDetails.getUserId(), q, cursor, size)
        ));
    }

    @Operation(summary = "Notion 기본 상위 페이지 설정")
    @PatchMapping
    ResponseEntity<ResponseNotionDefaultPage> updateDefaultPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RequestNotionDefaultPage request
    ) {
        return ResponseEntity.ok(ResponseNotionDefaultPage.from(
                notionPortIn.updateDefaultPage(new UpdateNotionDefaultPageCommand(
                        userDetails.getUserId(), request.defaultParentPageId(),
                        request.defaultParentPageTitle()
                ))
        ));
    }

    @Operation(summary = "Notion 연결 해제")
    @DeleteMapping
    ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notionPortIn.disconnect(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
