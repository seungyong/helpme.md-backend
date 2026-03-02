package seungyong.helpmebackend.adapter.out.result;

import java.time.Instant;
import java.util.List;

public record CommitResult(
    ContributorsResult.Contributor contributor,
    List<Commit> latestCommits,
    List<Commit> initialCommits,
    List<Commit> middleCommits
) {
    public record Commit(
            String sha,
            String message,
            Instant date
    ) {}
}
