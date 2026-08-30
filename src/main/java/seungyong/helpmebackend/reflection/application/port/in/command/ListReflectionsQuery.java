package seungyong.helpmebackend.reflection.application.port.in.command;

import java.time.LocalDate;

public record ListReflectionsQuery(
        Long userId,
        Long projectId,
        String kind,
        LocalDate from,
        LocalDate to,
        String status,
        String cursor,
        Integer size
) {
}
