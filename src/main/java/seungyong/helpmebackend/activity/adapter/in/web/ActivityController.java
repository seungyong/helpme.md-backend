package seungyong.helpmebackend.activity.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.activity.adapter.in.web.dto.response.ResponseActivities;
import seungyong.helpmebackend.activity.application.port.in.ActivityPortIn;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;

import java.time.LocalDate;

@Tag(name = "Activity", description = "프로젝트 활동 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/activities")
@RequiredArgsConstructor
@UserRoleApiErrors
class ActivityController {
    private final ActivityPortIn activityPortIn;

    @Operation(summary = "프로젝트 활동 조회")
    @GetMapping
    public ResponseEntity<ResponseActivities> getActivities(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ResponseActivities.from(activityPortIn.getActivities(
                userDetails.getUserId(), projectId, q, branch, type, from, to, cursor, size
        )));
    }
}
