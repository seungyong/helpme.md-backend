package seungyong.helpmebackend.adapter.in.web.dto.repository.response;

import seungyong.helpmebackend.domain.entity.Repository;

import java.util.ArrayList;

public record ResponseRepositories(
        ArrayList<Repository> repositories,
        int totalCount
) {
}
