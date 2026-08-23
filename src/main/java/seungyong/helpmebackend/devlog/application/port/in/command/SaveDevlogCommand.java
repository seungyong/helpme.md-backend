package seungyong.helpmebackend.devlog.application.port.in.command;

import java.time.LocalDate;

public record SaveDevlogCommand(
        Long userId,
        Long projectId,
        LocalDate logDate,
        String contentMarkdown,
        Integer version
) {
}
