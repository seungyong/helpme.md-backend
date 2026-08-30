package seungyong.helpmebackend.reflection.adapter.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import seungyong.helpmebackend.reflection.application.port.in.command.SaveReflectionCommand;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;

import java.util.List;

public record RequestSaveReflection(
        @NotBlank String title,
        @NotNull @Valid Content content,
        @NotNull @PositiveOrZero Integer version
) {
    public SaveReflectionCommand toCommand(
            Long userId, Long projectId, Long reflectionId
    ) {
        return new SaveReflectionCommand(
                userId, projectId, reflectionId, title, content.toDomain(), version
        );
    }

    public record Content(
            @NotNull @Min(1) @Max(1) Integer schemaVersion,
            @NotNull List<@NotNull @Valid Section> sections
    ) {
        private ReflectionDocument toDomain() {
            return new ReflectionDocument(
                    schemaVersion,
                    sections.stream().map(Section::toDomain).toList()
            );
        }
    }

    public record Section(
            @NotBlank String id,
            @NotBlank @Pattern(regexp = "markdown") String type,
            @NotBlank String title,
            @NotNull String contentMd,
            @NotNull List<@NotBlank String> evidenceRefs
    ) {
        private ReflectionDocument.Section toDomain() {
            return new ReflectionDocument.Section(
                    id, type, title, contentMd, evidenceRefs
            );
        }
    }
}
