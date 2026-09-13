package seungyong.helpmebackend.portfolio.adapter.out.storage;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportErrorCode;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioExportProcessingException;

import java.time.Duration;
import java.util.Map;

@Component
public class SupabasePortfolioStorageAdapter implements PortfolioStoragePortOut {
    private final RestClient.Builder restClientBuilder;

    @Value("${supabase.storage.base-url:}")
    private String baseUrl;

    @Value("${supabase.storage.service-role-key:}")
    private String serviceRoleKey;

    @Value("${supabase.storage.portfolio-bucket:portfolio-exports}")
    private String bucket;

    public SupabasePortfolioStorageAdapter(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public void upload(String path, byte[] content, String contentType) {
        execute(() -> client().post().uri("/object/{bucket}/{path}", bucket, path)
                .header("x-upsert", "true").contentType(MediaType.parseMediaType(contentType))
                .body(content).retrieve().toBodilessEntity());
    }

    @Override
    public String createSignedUrl(String path, Duration validFor) {
        JsonNode response = execute(() -> client().post().uri("/object/sign/{bucket}/{path}", bucket, path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("expiresIn", validFor.toSeconds())).retrieve().body(JsonNode.class));
        String signedUrl = response == null ? null : response.path("signedURL").asText(null);
        if (!StringUtils.hasText(signedUrl)) throw failure();
        return signedUrl.startsWith("http") ? signedUrl : baseUrl + signedUrl;
    }

    @Override
    public void delete(String path) {
        execute(() -> client().delete().uri("/object/{bucket}/{path}", bucket, path)
                .retrieve().toBodilessEntity());
    }

    private RestClient client() {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(serviceRoleKey)) throw failure();
        return restClientBuilder.baseUrl(baseUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey).build();
    }

    private <T> T execute(StorageRequest<T> request) {
        try {
            return request.execute();
        } catch (RestClientException exception) {
            throw failure();
        }
    }

    private PortfolioExportProcessingException failure() {
        return new PortfolioExportProcessingException(
                PortfolioExportErrorCode.PDF_RENDER_FAILED.getErrorCode(),
                PortfolioExportErrorCode.PDF_RENDER_FAILED.getMessage());
    }

    @FunctionalInterface
    private interface StorageRequest<T> {
        T execute();
    }
}
