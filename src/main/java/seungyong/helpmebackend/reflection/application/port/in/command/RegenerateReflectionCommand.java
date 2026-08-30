package seungyong.helpmebackend.reflection.application.port.in.command;

public record RegenerateReflectionCommand(
        Long userId,
        Long projectId,
        Long reflectionId,
        Boolean allowPartial
) {
}
