package seungyong.helpmebackend.repository.application.port.in.command;

public record GenerateDraftReadmeCommand(
        Long userId,
        String owner,
        String name,
        String branch,
        String taskId
) {
}
