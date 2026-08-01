package seungyong.helpmebackend.repository.application.port.in.result;

import seungyong.helpmebackend.repository.domain.entity.Repository;

import java.util.List;

public record RepositoryListResult(
        List<Repository> repositories,
        int totalCount
) {
}
