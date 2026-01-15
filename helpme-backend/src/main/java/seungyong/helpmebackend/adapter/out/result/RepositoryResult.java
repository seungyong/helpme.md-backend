package seungyong.helpmebackend.adapter.out.result;

import seungyong.helpmebackend.domain.entity.Repository;

import java.util.ArrayList;

public record RepositoryResult(
        ArrayList<Repository> repositories,
        Integer totalCount
) {
}
