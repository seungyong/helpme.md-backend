package seungyong.helpmebackend.adapter.out.command;

public record RepoBranchCommand(
        RepoInfoCommand repoInfo,
        String branch
) {
}
