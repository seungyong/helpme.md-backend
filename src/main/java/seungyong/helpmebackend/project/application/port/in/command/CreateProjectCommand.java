package seungyong.helpmebackend.project.application.port.in.command;

import java.util.List;

public record CreateProjectCommand(
        Long userId,
        Long githubInstallationId,
        Long githubRepoId,
        String defaultBranch,
        List<String> trackedBranches,
        boolean trackAllBranches,
        String timezone
) {
    public CreateProjectCommand {
        trackedBranches = trackedBranches == null ? List.of() : List.copyOf(trackedBranches);
    }
}
