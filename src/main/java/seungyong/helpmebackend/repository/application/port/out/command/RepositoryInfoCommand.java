package seungyong.helpmebackend.repository.application.port.out.command;

import seungyong.helpmebackend.repository.application.port.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;

import java.util.List;

public record RepositoryInfoCommand(
        List<RepositoryLanguageResult> languages,
        List<CommitCommand> commits,
        List<RepositoryTreeResult> trees
) {
    public record ContributorCommand(
            String username,
            String avatarUrl
    ) {}

    public record CommitCommand(
            ContributorCommand contributor,
            List<String> latestCommit,
            List<String> middleCommit,
            List<String> initialCommit
    ) {}
}
