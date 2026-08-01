package seungyong.helpmebackend.repository.application.port.in.command;

public record EvaluateDraftReadmeCommand(
        Long userId,
        String owner,
        String name,
        String branch,
        String content,
        String taskId
) {
}
