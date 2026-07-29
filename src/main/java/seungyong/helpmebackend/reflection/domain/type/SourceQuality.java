package seungyong.helpmebackend.reflection.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum SourceQuality implements DatabaseValueEnum {
    COMPLETE("complete"),
    PARTIAL("partial");

    private final String databaseValue;
}
