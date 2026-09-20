package seungyong.helpmebackend.project.application.port.in.command;

public record DeleteProjectCommand(
        Long userId,
        Long projectId,
        String confirmationRepoFullname
) {
}
