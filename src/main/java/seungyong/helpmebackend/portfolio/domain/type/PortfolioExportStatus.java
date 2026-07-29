package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum PortfolioExportStatus implements DatabaseValueEnum {
    QUEUED("queued"),
    PROCESSING("processing"),
    NEEDS_ACTION("needs_action"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    EXPIRED("expired");

    private final String databaseValue;
}
