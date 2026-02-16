package seungyong.helpmebackend.adapter.in.web.dto.repository.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResponsePull(
        @Schema(description = "Pull Request Github URL", example = "https://github.com/octocat/Hello-World/pull/1347")
        String htmlUrl
) {
}
