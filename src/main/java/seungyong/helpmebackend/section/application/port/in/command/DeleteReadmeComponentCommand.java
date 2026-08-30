package seungyong.helpmebackend.section.application.port.in.command;

public record DeleteReadmeComponentCommand(
        Long userId,
        String owner,
        String name,
        Long componentId,
        Integer version
) {
}
