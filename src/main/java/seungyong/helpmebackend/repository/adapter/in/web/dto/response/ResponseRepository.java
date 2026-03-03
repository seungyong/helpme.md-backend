package seungyong.helpmebackend.repository.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "레포지토리 정보 응답 DTO")
public record ResponseRepository(
        @Schema(description = "레포지토리 ID", example = "123456789")
        String owner,
        @Schema(description = "레포지토리 이름", example = "Hello-World")
        String name,
        @Schema(description = "레포지토리 사진", example = "https://avatars.githubusercontent.com/u/583231?v=4")
        String avatarUrl,
        @Schema(description = "레포지토리 기본 브랜치", example = "main")
        String defaultBranch
) {
}
