package seungyong.helpmebackend.user.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum GithubTokenStatus implements DatabaseValueEnum {
    UNKNOWN("unknown"),
    VALID("valid"),
    REVOKED("revoked");

    private final String databaseValue;
}
