package seungyong.helpmebackend.adapter.out.command;

public record CommonRepoCommand(
        String accessToken,
        String owner,
        String name
) {
}
