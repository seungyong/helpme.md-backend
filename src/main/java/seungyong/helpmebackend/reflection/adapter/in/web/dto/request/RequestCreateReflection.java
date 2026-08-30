package seungyong.helpmebackend.reflection.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import seungyong.helpmebackend.reflection.application.port.in.command.CreateReflectionCommand;

import java.time.LocalDate;

public record RequestCreateReflection(
        @NotNull
        @Pattern(regexp = "daily|weekly")
        String kind,
        @NotNull
        LocalDate periodStart,
        @Schema(defaultValue = "blank", allowableValues = {"ai", "blank"})
        @Pattern(regexp = "ai|blank")
        String generationMode,
        Boolean allowPartial
) {
    public CreateReflectionCommand toCommand(Long userId, Long projectId) {
        return new CreateReflectionCommand(
                userId, projectId, kind, periodStart, generationMode, allowPartial
        );
    }
}
