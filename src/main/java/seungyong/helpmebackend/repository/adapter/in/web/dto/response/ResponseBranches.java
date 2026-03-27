package seungyong.helpmebackend.repository.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "레포지토리 브랜치 목록 응답 DTO")
public record ResponseBranches(
        @Schema(description = "기본 브랜치 이름", example = "main")
        String defaultBranch,
        @Schema(description = "브랜치 이름 목록", example = "[\"main\", \"develop\", \"feature-branch\"]")
        List<String> branches
) {
}
