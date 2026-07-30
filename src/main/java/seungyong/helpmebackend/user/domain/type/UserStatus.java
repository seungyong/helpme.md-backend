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

    /**
     * 사용자 상태가 활성화된 상태인지 확인하는 메소드
     * @return true는 활성화 상태, false는 삭제중 또는 삭제 실패 상태
     */
    public boolean allowsAuthentication() {
        return this == ACTIVE;
    }
}
