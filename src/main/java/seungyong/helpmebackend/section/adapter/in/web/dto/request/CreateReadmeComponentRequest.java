package seungyong.helpmebackend.section.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateReadmeComponentRequest(
        @NotBlank String title,
        String content,
        @PositiveOrZero Integer orderIdx
) {
}
