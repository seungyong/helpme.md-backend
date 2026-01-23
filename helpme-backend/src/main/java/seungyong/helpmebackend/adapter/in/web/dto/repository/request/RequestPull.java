package seungyong.helpmebackend.adapter.in.web.dto.repository.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RequestPull(
        @Schema(description = "README 내용", example = "This is a sample README content.")
        @NotBlank(message = "README 내용은 필수입니다.")
        String content
) {
}
