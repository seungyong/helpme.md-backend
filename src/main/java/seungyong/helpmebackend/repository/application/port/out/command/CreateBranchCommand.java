package seungyong.helpmebackend.repository.application.port.out.command;

public record CreateBranchCommand(
        RepoInfoCommand repoInfo,
        String newBranchName,
        String sha
) {
}
