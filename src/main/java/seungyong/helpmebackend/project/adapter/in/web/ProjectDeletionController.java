package seungyong.helpmebackend.project.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.project.adapter.in.web.dto.request.RequestDeleteProject;
import seungyong.helpmebackend.project.adapter.in.web.dto.response.ResponseProjectDeletion;
import seungyong.helpmebackend.project.application.port.in.ProjectDeletionPortIn;
import seungyong.helpmebackend.project.application.port.in.command.DeleteProjectCommand;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@UserRoleApiErrors
class ProjectDeletionController {
    private final ProjectDeletionPortIn projectDeletionPortIn;

    @Operation(
            summary = "프로젝트 연결 삭제",
            description = "프로젝트를 즉시 deleting으로 전환하고 외부 자산 정리를 시작합니다.",
            responses = @ApiResponse(responseCode = "202", description = "삭제 요청 접수")
    )
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ResponseProjectDeletion> deleteProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody RequestDeleteProject request
    ) {
        return ResponseEntity.accepted().body(ResponseProjectDeletion.from(
                projectDeletionPortIn.requestDeletion(new DeleteProjectCommand(
                        userDetails.getUserId(), projectId, request.confirmationRepoFullname()
                ))
        ));
    }
}
