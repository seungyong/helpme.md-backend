package seungyong.helpmebackend.repository.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "레포지토리 평가 요청 DTO")
public record RequestEvaluation(
        @Schema(description = "브랜치 이름 (평가할 기준 브랜치)", example = "main")
        @NotBlank(message = "브랜치 이름은 필수입니다.")
        String branch
) {
}
