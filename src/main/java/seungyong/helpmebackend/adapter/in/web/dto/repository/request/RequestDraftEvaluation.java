package seungyong.helpmebackend.adapter.in.web.dto.repository.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "레포지토리 평가 요청 DTO")
public record RequestDraftEvaluation(
        @Schema(description = "브랜치 이름 (평가할 기준 브랜치)", example = "main")
        @NotBlank(message = "브랜치 이름은 필수입니다.")
        String branch,

        @Schema(description = "README 내용", example = "## 프로젝트 소개\n이 프로젝트는 ...")
        @NotBlank(message = "README 내용은 필수입니다.")
        String content
) {
}
