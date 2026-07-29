package seungyong.helpmebackend.user.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum PlanCode implements DatabaseValueEnum {
    FREE("free"),
    PRO("pro"),
    ADMIN("admin");

    private final String databaseValue;
}
