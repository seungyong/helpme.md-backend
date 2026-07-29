package seungyong.helpmebackend.notion.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum NotionConnectionStatus implements DatabaseValueEnum {
    CONNECTED("connected"),
    RECONNECT_REQUIRED("reconnect_required"),
    REVOKED("revoked"),
    ERROR("error");

    private final String databaseValue;
}
