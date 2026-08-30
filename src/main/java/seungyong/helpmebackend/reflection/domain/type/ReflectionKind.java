package seungyong.helpmebackend.reflection.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ReflectionKind implements DatabaseValueEnum {
    DAILY("daily"),
    WEEKLY("weekly");

    private final String databaseValue;

    public static ReflectionKind fromDatabaseValue(String value) {
        return Arrays.stream(values())
                .filter(kind -> kind.databaseValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown reflection kind: " + value
                ));
    }
}
