package seungyong.helpmebackend.adapter.in.web.dto.section.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestSectionContent(
        @NotNull(message = "섹션 ID는 필수입니다.")
        Long id,
        @NotBlank(message = "섹션 내용은 필수입니다.")
        String content
) {
}
