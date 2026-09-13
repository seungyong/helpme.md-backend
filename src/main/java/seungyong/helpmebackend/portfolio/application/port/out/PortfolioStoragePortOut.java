package seungyong.helpmebackend.portfolio.application.port.out;

import java.time.Duration;

public interface PortfolioStoragePortOut {
    void upload(String path, byte[] content, String contentType);
    String createSignedUrl(String path, Duration validFor);
    void delete(String path);
}
