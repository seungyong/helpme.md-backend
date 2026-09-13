package seungyong.helpmebackend.portfolio.adapter.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import seungyong.helpmebackend.portfolio.application.port.in.command.StartPortfolioExportCommand;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportOptions;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;

import java.util.UUID;

public record RequestStartPortfolioExport(
        @NotBlank @Pattern(regexp = "pdf|notion") String format,
        @NotNull @Min(0) Integer portfolioVersion,
        String notionParentPageId,
        @Valid Options options
) {
    public StartPortfolioExportCommand toCommand(Long userId, Long projectId, Long portfolioId,
                                                  UUID idempotencyKey) {
        PortfolioExportFormat parsedFormat = PortfolioExportFormat.fromDatabaseValue(format);
        PortfolioExportOptions defaults = PortfolioExportOptions.defaults(parsedFormat);
        PortfolioExportOptions normalized = options == null ? defaults : new PortfolioExportOptions(
                value(options.includeCoverAndToc, defaults.includeCoverAndToc()),
                value(options.includeEvidenceLinks, defaults.includeEvidenceLinks()),
                value(options.includePageNumbers, defaults.includePageNumbers()),
                value(options.showGeneratedAt, defaults.showGeneratedAt()),
                value(options.checkTitleConflict, defaults.checkTitleConflict())
        );
        return new StartPortfolioExportCommand(userId, projectId, portfolioId, idempotencyKey,
                parsedFormat, portfolioVersion, notionParentPageId, normalized);
    }

    private boolean value(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    public record Options(Boolean includeCoverAndToc, Boolean includeEvidenceLinks,
                          Boolean includePageNumbers, Boolean showGeneratedAt,
                          Boolean checkTitleConflict) {
    }
}
