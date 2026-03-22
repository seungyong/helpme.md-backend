package seungyong.helpmebackend.repository.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pull Request 생성 요청 DTO")
public record RequestPull(
        @Schema(description = "머지될 brach 이름", example = "feature/update-readme")
        @NotBlank(message = "branch 이름은 필수입니다.")
        String branch,

        @Schema(description = "README 내용", example = "This is a sample README content.")
        @NotBlank(message = "README 내용은 필수입니다.")
        String content
) {
}
