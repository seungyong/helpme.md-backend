package seungyong.helpmebackend.repository.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import seungyong.helpmebackend.repository.domain.entity.Repository;

import java.util.List;

@Schema(description = "레포지토리 목록 응답 DTO")
public record ResponseRepositories(
        @Schema(
                description = "레포지토리 목록",
                example = "[{ \"installationId\": 1, \"avatarUrl\": \"https://avatars.githubusercontent.com/u/12345678?v=4\", \"name\": \"octocat/Hello-World\" }]"
        )
        List<Repository> repositories,
        int totalCount
) {
}
