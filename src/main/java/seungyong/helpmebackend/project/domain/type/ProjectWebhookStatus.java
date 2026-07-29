package seungyong.helpmebackend.project.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum ProjectWebhookStatus implements DatabaseValueEnum {
    WAITING("waiting"),
    HEALTHY("healthy"),
    DEGRADED("degraded"),
    DISCONNECTED("disconnected");

    private final String databaseValue;
}
