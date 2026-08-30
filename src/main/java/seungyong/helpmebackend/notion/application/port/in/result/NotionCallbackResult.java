package seungyong.helpmebackend.notion.application.port.in.result;

import lombok.Getter;

public record NotionCallbackResult(
        String returnUrl,
        Outcome outcome,
        String errorCode
) {
    public static NotionCallbackResult success(String returnUrl) {
        return new NotionCallbackResult(returnUrl, Outcome.SUCCESS, null);
    }

    public static NotionCallbackResult denied(String returnUrl) {
        return new NotionCallbackResult(returnUrl, Outcome.DENIED, null);
    }

    public static NotionCallbackResult error(String returnUrl, String errorCode) {
        return new NotionCallbackResult(returnUrl, Outcome.ERROR, errorCode);
    }

    @Getter
    public enum Outcome {
        SUCCESS("success"),
        DENIED("denied"),
        ERROR("error");

        private final String queryValue;

        Outcome(String queryValue) {
            this.queryValue = queryValue;
        }

    }
}
