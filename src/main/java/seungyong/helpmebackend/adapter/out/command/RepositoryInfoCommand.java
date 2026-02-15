package seungyong.helpmebackend.adapter.out.command;

import seungyong.helpmebackend.adapter.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;

import java.util.List;

public record RepositoryInfoCommand(
        List<RepositoryLanguageResult> languages,
        CommitCommand commits,
        List<RepositoryTreeResult> trees
) {
    public record CommitCommand(
            List<String> latestCommit,
            List<String> middleCommit,
            List<String> initialCommit
    ) {}
}
