package seungyong.helpmebackend.adapter.in.web.dto.repository.response;

import seungyong.helpmebackend.domain.entity.repository.Repository;

import java.util.List;

public record ResponseRepositories(
        List<Repository> repositories,
        int totalCount
) {
}
