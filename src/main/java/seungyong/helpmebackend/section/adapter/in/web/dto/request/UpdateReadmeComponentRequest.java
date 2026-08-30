package seungyong.helpmebackend.section.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateReadmeComponentRequest(
        String title,
        String content,
        @PositiveOrZero Integer orderIdx,
        @NotNull @PositiveOrZero Integer version
) {
}
