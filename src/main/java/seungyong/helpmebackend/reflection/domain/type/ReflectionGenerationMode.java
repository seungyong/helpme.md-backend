package seungyong.helpmebackend.reflection.domain.type;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ReflectionGenerationMode {
    AI("ai"),
    BLANK("blank");

    private final String apiValue;

    ReflectionGenerationMode(String apiValue) {
        this.apiValue = apiValue;
    }

    public static ReflectionGenerationMode fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.apiValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown reflection generation mode: " + value
                ));
    }
}
