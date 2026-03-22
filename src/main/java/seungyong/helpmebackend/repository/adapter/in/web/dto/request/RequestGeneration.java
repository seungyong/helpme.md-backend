package seungyong.helpmebackend.repository.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "README 초안 생성 DTO")
public record RequestGeneration(
        @Schema(description = "브랜치 이름 (기준 브랜치)", example = "main")
        @NotBlank(message = "브랜치 이름은 필수입니다.")
        String branch
) {
}
