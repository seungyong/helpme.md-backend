package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import seungyong.helpmebackend.adapter.in.web.dto.component.request.RequestComponent;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseComponents;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseCreatedComponent;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.usecase.port.in.component.ComponentPortIn;

@Tag(
        name = "Component",
        description = """
                Component 관련 API
                """
)
@RestController
@RequestMapping("/api/v1")
@ResponseBody
@RequiredArgsConstructor
public class ComponentController {
    private final ComponentPortIn componentPortIn;

    @GetMapping("/repos/{owner}/{name}/components")
    public ResponseEntity<ResponseComponents> getComponents(
            @PathVariable String owner,
            @PathVariable String name,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        return ResponseEntity.ok(
                componentPortIn.getComponents(owner, name, userDetails.getUserId())
        );
    }

    @PostMapping("/repos/{owner}/{name}/components")
    public ResponseEntity<ResponseCreatedComponent> createComponent(
            @RequestBody @Valid RequestComponent request,
            @PathVariable String owner,
            @PathVariable String name,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                componentPortIn.createComponent(request, owner, name, userDetails.getUserId())
        );
    }

    @PatchMapping("/components/{componentId}")
    public ResponseEntity<Void> updateComponent(
            @RequestBody @Valid RequestComponent request,
            @PathVariable Long componentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        componentPortIn.updateComponent(request, componentId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/components/{componentId}")
    public ResponseEntity<Void> deleteComponent(
            @PathVariable Long componentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        componentPortIn.deleteComponent(componentId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
