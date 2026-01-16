package seungyong.helpmebackend.adapter.out.result;

import seungyong.helpmebackend.domain.entity.repository.Repository;

import java.util.List;

public record RepositoryResult(
        List<Repository> repositories,
        Integer totalCount
) {
}
