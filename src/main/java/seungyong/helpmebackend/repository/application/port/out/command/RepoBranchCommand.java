package seungyong.helpmebackend.repository.application.port.out.command;

public record RepoBranchCommand(
        RepoInfoCommand repoInfo,
        String branch
) {
}
