package seungyong.helpmebackend.notion.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.notion.application.port.in.NotionPortIn;
import seungyong.helpmebackend.notion.application.port.in.result.NotionCallbackResult;

import java.net.URI;

@Tag(name = "Notion OAuth", description = "Notion이 호출하는 공개 OAuth callback")
@RestController
@RequestMapping("/api/v1/integrations/notion/oauth")
@RequiredArgsConstructor
class NotionOAuthCallbackController {
    private final NotionPortIn notionPortIn;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Operation(summary = "Notion OAuth callback")
    @GetMapping("/callback")
    ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "error") String providerError
    ) {
        NotionCallbackResult result = notionPortIn.handleCallback(code, state, providerError);
        String location = frontendUrl + "/#" + result.returnUrl()
                + "?notion=" + result.outcome().getQueryValue()
                + (result.errorCode() == null ? "" : "&code=" + result.errorCode());
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, URI.create(location).toASCIIString())
                .build();
    }
}
