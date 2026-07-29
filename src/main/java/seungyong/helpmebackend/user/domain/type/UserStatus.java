package seungyong.helpmebackend.user.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum UserStatus implements DatabaseValueEnum {
    ACTIVE("active"),
    DELETING("deleting"),
    DELETE_FAILED("delete_failed");

    private final String databaseValue;
}
