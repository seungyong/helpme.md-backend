package seungyong.helpmebackend.project.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum ProjectSyncStatus implements DatabaseValueEnum {
    PENDING("pending"),
    RUNNING("running"),
    READY("ready"),
    FAILED("failed");

    private final String databaseValue;
}
