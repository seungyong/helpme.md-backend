package seungyong.helpmebackend.webhook.application.port.out;

public interface WebhookWorkPortOut {
    void registerInitialSync(Long projectId);

    void retryInitialSync(Long projectId);
}
