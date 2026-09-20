package seungyong.helpmebackend.user.application.port.in.command;

public record DeleteUserCommand(
        Long userId,
        boolean confirmed,
        String refreshToken
) {
}
