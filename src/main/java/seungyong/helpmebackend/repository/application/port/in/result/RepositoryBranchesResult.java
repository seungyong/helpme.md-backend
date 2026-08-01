package seungyong.helpmebackend.repository.application.port.in.result;

import java.util.List;

public record RepositoryBranchesResult(
        String defaultBranch,
        List<String> branches
) {
}
