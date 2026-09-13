package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PortfolioExportFormat implements DatabaseValueEnum {
    PDF("pdf"),
    NOTION("notion");

    private final String databaseValue;

    public static PortfolioExportFormat fromDatabaseValue(String value) {
        return Arrays.stream(values()).filter(item -> item.databaseValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported portfolio export format"));
    }
}
