package seungyong.helpmebackend.auth.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import seungyong.helpmebackend.auth.domain.entity.Installation;

import java.util.List;

@Schema(description = "Github App이 설치된 Repository 응답 DTO")
public record ResponseInstallations(
        @Schema(
                description = "Repository 목록",
                example = "[{ \"installationId\": 1, \"avatarUrl\": \"https://avatars.githubusercontent.com/u/12345678?v=4\", \"name\": \"octocat/Hello-World\" }]"
        )
        List<Installation> installations
) {
}
