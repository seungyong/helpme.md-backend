package seungyong.helpmebackend.portfolio.adapter.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import seungyong.helpmebackend.portfolio.application.port.in.command.SavePortfolioCommand;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;

import java.util.List;

public record RequestSavePortfolio(
        @NotBlank String title,
        @NotBlank @Pattern(regexp = "concise|reflection") String tone,
        @NotNull @Valid Content content,
        @NotNull @PositiveOrZero Integer version
) {
    public SavePortfolioCommand toCommand(Long userId, Long projectId, Long portfolioId) {
        return new SavePortfolioCommand(userId, projectId, portfolioId, title, tone, content.toDomain(), version);
    }

    public record Content(@NotNull @Min(1) @Max(1) Integer schemaVersion,
                          @NotNull List<@NotNull @Valid Section> sections) {
        private PortfolioDocument toDomain() {
            return new PortfolioDocument(schemaVersion, sections.stream().map(Section::toDomain).toList());
        }
    }

    public record Section(@NotBlank String id, @NotBlank String type, @NotBlank String title,
                          @NotNull String contentMd, @NotNull List<@NotBlank String> evidenceRefs) {
        private PortfolioDocument.Section toDomain() {
            return new PortfolioDocument.Section(id, type, title, contentMd, evidenceRefs);
        }
    }
}
