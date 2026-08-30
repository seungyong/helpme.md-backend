package seungyong.helpmebackend.reflection.adapter.in.web.dto.response;

import seungyong.helpmebackend.reflection.domain.entity.ReflectionGenerationResult;

public record ResponseReflectionGeneration(
        Long reflectionId,
        String status,
        String location,
        int retryAfterSeconds
) {
    public static ResponseReflectionGeneration from(
            Long projectId, ReflectionGenerationResult result
    ) {
        return new ResponseReflectionGeneration(
                result.reflectionId(),
                result.status().getDatabaseValue(),
                "/api/v1/projects/" + projectId + "/reflections/" + result.reflectionId(),
                result.retryAfterSeconds()
        );
    }
}
