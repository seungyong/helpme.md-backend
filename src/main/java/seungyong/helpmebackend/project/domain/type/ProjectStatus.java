package seungyong.helpmebackend.project.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum ProjectStatus implements DatabaseValueEnum {
    ACTIVE("active"),
    DELETING("deleting"),
    DELETE_FAILED("delete_failed");

    private final String databaseValue;
}
