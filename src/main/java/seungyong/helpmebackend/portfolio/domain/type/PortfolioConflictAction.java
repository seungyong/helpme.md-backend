package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PortfolioConflictAction implements DatabaseValueEnum {
    UPDATE("update"),
    COPY("copy");

    private final String databaseValue;

    public static PortfolioConflictAction fromDatabaseValue(String value) {
        return Arrays.stream(values()).filter(item -> item.databaseValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported portfolio conflict action"));
    }
}
