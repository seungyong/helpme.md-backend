package seungyong.helpmebackend.project.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import seungyong.helpmebackend.project.application.port.in.command.CreateProjectCommand;

import java.util.List;

public record RequestCreateProject(
        @NotNull @Positive Long githubInstallationId,
        @NotNull @Positive Long githubRepoId,
        @NotBlank String defaultBranch,
        @NotNull @Size(max = 100) List<@NotBlank String> trackedBranches,
        boolean trackAllBranches,
        @NotBlank String timezone
) {
    public CreateProjectCommand toCommand(Long userId) {
        return new CreateProjectCommand(
                userId, githubInstallationId, githubRepoId, defaultBranch,
                trackedBranches, trackAllBranches, timezone
        );
    }
}
