package seungyong.helpmebackend.reflection.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum ReflectionKind implements DatabaseValueEnum {
    DAILY("daily"),
    WEEKLY("weekly");

    private final String databaseValue;
}
