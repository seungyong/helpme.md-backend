package seungyong.helpmebackend.reflection.adapter.in.web.dto.response;

import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;

import java.time.OffsetDateTime;

public record ResponseSavedReflection(
        Long id,
        String title,
        ReflectionDocument content,
        String status,
        int version,
        OffsetDateTime savedAt,
        OffsetDateTime updatedAt
) {
    public static ResponseSavedReflection from(Reflection reflection) {
        return new ResponseSavedReflection(
                reflection.id(),
                reflection.title(),
                reflection.content(),
                reflection.status().getDatabaseValue(),
                reflection.version(),
                reflection.savedAt(),
                reflection.updatedAt()
        );
    }
}
