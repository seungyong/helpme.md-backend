package seungyong.helpmebackend.notion.application.port.in.command;

public record UpdateNotionDefaultPageCommand(
        Long userId,
        String defaultParentPageId,
        String defaultParentPageTitle
) {
}
