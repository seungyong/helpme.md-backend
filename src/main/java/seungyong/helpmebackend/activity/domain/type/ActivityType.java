package seungyong.helpmebackend.activity.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ActivityType implements DatabaseValueEnum {
    PUSH_COMMIT("push_commit"),
    PULL_REQUEST("pull_request");

    private final String databaseValue;

    public static ActivityType fromDatabaseValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.databaseValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported activity type"));
    }
}
