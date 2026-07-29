package seungyong.helpmebackend.activity.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum ActivityType implements DatabaseValueEnum {
    PUSH_COMMIT("push_commit"),
    PULL_REQUEST("pull_request");

    private final String databaseValue;
}
