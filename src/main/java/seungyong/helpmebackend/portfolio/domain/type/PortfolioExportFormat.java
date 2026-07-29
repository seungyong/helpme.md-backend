package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum PortfolioExportFormat implements DatabaseValueEnum {
    PDF("pdf"),
    NOTION("notion");

    private final String databaseValue;
}
