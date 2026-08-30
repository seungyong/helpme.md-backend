package seungyong.helpmebackend.reflection.adapter.in.web.dto.request;

import seungyong.helpmebackend.reflection.application.port.in.command.RegenerateReflectionCommand;

public record RequestRegenerateReflection(Boolean allowPartial) {
    public RegenerateReflectionCommand toCommand(
            Long userId, Long projectId, Long reflectionId
    ) {
        return new RegenerateReflectionCommand(
                userId, projectId, reflectionId, allowPartial
        );
    }
}
