package seungyong.helpmebackend.webhook.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum WebhookDeliveryStatus implements DatabaseValueEnum {
    RECEIVED("received"),
    PROCESSING("processing"),
    PROCESSED("processed"),
    IGNORED("ignored"),
    FAILED("failed");

    private final String databaseValue;
}
