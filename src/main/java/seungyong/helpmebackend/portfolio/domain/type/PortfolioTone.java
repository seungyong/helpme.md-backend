package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PortfolioTone implements DatabaseValueEnum {
    CONCISE("concise"),
    REFLECTION("reflection");

    private final String databaseValue;

    public static PortfolioTone fromDatabaseValue(String value) {
        return Arrays.stream(values())
                .filter(tone -> tone.databaseValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported portfolio tone"));
    }
}
