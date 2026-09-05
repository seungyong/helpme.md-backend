package seungyong.helpmebackend.portfolio.adapter.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import seungyong.helpmebackend.portfolio.application.port.in.command.CreatePortfolioCommand;
import seungyong.helpmebackend.portfolio.application.port.in.command.CustomEvidenceLinkCommand;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RequestCreatePortfolio(
        @NotBlank String title,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotBlank @Pattern(regexp = "concise|reflection") String tone,
        @NotNull List<@NotNull Long> reflectionIds,
        List<@NotNull Long> activityIds,
        List<@NotNull @Valid CustomEvidenceLink> customEvidenceLinks,
        @Pattern(regexp = "ai|blank") String generationMode
) {
    public CreatePortfolioCommand toCommand(Long userId, Long projectId, UUID idempotencyKey) {
        return new CreatePortfolioCommand(
                userId, projectId, idempotencyKey, title, periodStart, periodEnd, tone,
                reflectionIds,
                activityIds == null ? List.of() : activityIds,
                customEvidenceLinks == null ? List.of()
                        : customEvidenceLinks.stream().map(CustomEvidenceLink::toCommand).toList(),
                generationMode
        );
    }

    public record CustomEvidenceLink(@NotBlank String label, @NotBlank String url) {
        private CustomEvidenceLinkCommand toCommand() {
            return new CustomEvidenceLinkCommand(label, url);
        }
    }
}
