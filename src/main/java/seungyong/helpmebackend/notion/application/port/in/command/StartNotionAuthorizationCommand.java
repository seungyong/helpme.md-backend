package seungyong.helpmebackend.notion.application.port.in.command;

public record StartNotionAuthorizationCommand(Long userId, String returnUrl) {
}
