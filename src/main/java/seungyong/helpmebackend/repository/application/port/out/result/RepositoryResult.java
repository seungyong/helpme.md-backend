package seungyong.helpmebackend.repository.application.port.out.result;

import seungyong.helpmebackend.repository.domain.entity.Repository;

import java.util.List;

public record RepositoryResult(
        List<Repository> repositories,
        Integer totalCount
) {
}
