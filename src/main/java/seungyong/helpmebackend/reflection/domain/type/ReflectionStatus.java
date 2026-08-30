package seungyong.helpmebackend.reflection.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ReflectionStatus implements DatabaseValueEnum {
    QUEUED("queued"),
    GENERATING("generating"),
    DRAFT("draft"),
    SAVED("saved"),
    FAILED("failed");

    private final String databaseValue;

    public static ReflectionStatus fromDatabaseValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.databaseValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown reflection status: " + value
                ));
    }
}
