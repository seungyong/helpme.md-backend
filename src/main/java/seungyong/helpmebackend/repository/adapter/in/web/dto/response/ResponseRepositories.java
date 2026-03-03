package seungyong.helpmebackend.repository.adapter.in.web.dto.response;

import seungyong.helpmebackend.repository.domain.entity.Repository;

import java.util.List;

public record ResponseRepositories(
        List<Repository> repositories,
        int totalCount
) {
}
