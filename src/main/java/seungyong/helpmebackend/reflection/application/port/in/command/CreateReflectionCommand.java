package seungyong.helpmebackend.reflection.application.port.in.command;

import java.time.LocalDate;

public record CreateReflectionCommand(
        Long userId,
        Long projectId,
        String kind,
        LocalDate periodStart,
        String generationMode,
        Boolean allowPartial
) {
}
