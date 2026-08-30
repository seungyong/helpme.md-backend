package seungyong.helpmebackend.reflection.domain.entity;

import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

public record ReflectionGenerationResult(
        Long reflectionId,
        ReflectionStatus status,
        boolean created,
        boolean asynchronous,
        int retryAfterSeconds
) {
}
