package seungyong.helpmebackend.project.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ReflectionWeekday {
    SUNDAY("sunday", (short) 0),
    MONDAY("monday", (short) 1),
    TUESDAY("tuesday", (short) 2),
    WEDNESDAY("wednesday", (short) 3),
    THURSDAY("thursday", (short) 4),
    FRIDAY("friday", (short) 5),
    SATURDAY("saturday", (short) 6);

    private final String apiValue;
    private final short databaseValue;

    public static ReflectionWeekday fromApiValue(String apiValue) {
        return Arrays.stream(values())
                .filter(value -> value.apiValue.equals(apiValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown reflection weekday API value: " + apiValue
                ));
    }

    public static ReflectionWeekday fromDatabaseValue(short databaseValue) {
        return Arrays.stream(values())
                .filter(value -> value.databaseValue == databaseValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown reflection weekday database value: " + databaseValue
                ));
    }
}
