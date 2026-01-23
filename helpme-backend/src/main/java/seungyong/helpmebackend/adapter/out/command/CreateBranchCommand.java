package seungyong.helpmebackend.adapter.out.command;

public record CreateBranchCommand(
        RepoInfoCommand repoInfo,
        String newBranchName,
        String sha
) {
}
