package seungyong.helpmebackend.user.adapter.in.web.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record RequestUserDeletion(
        @NotNull
        @AssertTrue
        Boolean confirmed
) {
}
