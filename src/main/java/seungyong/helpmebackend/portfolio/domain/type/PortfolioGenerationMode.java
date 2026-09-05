package seungyong.helpmebackend.portfolio.domain.type;

import java.util.Arrays;

public enum PortfolioGenerationMode {
    AI("ai"),
    BLANK("blank");

    private final String apiValue;

    PortfolioGenerationMode(String apiValue) {
        this.apiValue = apiValue;
    }

    public static PortfolioGenerationMode fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.apiValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported generation mode"));
    }
}
