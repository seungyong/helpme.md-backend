package seungyong.helpmebackend.section.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DeleteReadmeComponentRequest(
        @NotNull @PositiveOrZero Integer version
) {
}
