package seungyong.helpmebackend.portfolio.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;

public record RequestPortfolioConflictResolution(
        @NotBlank @Pattern(regexp = "update|copy") String action
) {
    public PortfolioConflictAction toAction() {
        return PortfolioConflictAction.fromDatabaseValue(action);
    }
}
