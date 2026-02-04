package seungyong.helpmebackend.adapter.in.web.dto.section.request;

import jakarta.validation.constraints.NotBlank;

public record RequestSection(
        @NotBlank(message = "Section 제목은 비어 있을 수 없습니다.")
        String title
) {
}
