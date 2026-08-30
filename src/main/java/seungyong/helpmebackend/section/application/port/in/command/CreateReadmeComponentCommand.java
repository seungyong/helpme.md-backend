package seungyong.helpmebackend.section.application.port.in.command;

public record CreateReadmeComponentCommand(
        Long userId,
        String owner,
        String name,
        String title,
        String content,
        Integer orderIdx
) {
}
