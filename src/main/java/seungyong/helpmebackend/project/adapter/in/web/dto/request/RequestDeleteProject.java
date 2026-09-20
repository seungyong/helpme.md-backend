package seungyong.helpmebackend.project.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RequestDeleteProject(
        @NotBlank String confirmationRepoFullname
) {
}
