package seungyong.helpmebackend.section.application.port.in.command;

public record UpdateReadmeComponentCommand(
        Long userId,
        String owner,
        String name,
        Long componentId,
        String title,
        String content,
        Integer orderIdx,
        Integer version
) {
}
