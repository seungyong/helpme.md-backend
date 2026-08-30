package seungyong.helpmebackend.reflection.application.port.in.command;

import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;

public record SaveReflectionCommand(
        Long userId,
        Long projectId,
        Long reflectionId,
        String title,
        ReflectionDocument content,
        Integer version
) {
}
