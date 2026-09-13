package seungyong.helpmebackend.notion.adapter.out.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import seungyong.helpmebackend.notion.application.port.out.NotionProviderPortOut;
import seungyong.helpmebackend.notion.application.port.out.exception.NotionProviderException;
import seungyong.helpmebackend.notion.application.port.out.result.NotionOAuthGrant;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPage;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPages;
import seungyong.helpmebackend.notion.application.port.out.result.NotionRefreshedTokens;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotionApiAdapter implements NotionProviderPortOut,
        seungyong.helpmebackend.portfolio.application.port.out.PortfolioNotionProviderPortOut {
    private static final String AUTHORIZATION_URL = "https://api.notion.com/v1/oauth/authorize";

    private final RestClient.Builder restClientBuilder;

    @Value("${oauth2.notion.client-id:}")
    private String clientId;

    @Value("${oauth2.notion.client-secret:}")
    private String clientSecret;

    @Value("${oauth2.notion.redirect-uri:}")
    private String redirectUri;

    @Value("${oauth2.notion.api-base-url:https://api.notion.com}")
    private String apiBaseUrl;

    @Value("${oauth2.notion.api-version:2026-03-11}")
    private String apiVersion;

    @Override
    public String buildAuthorizationUrl(String state) {
        requireOAuthConfiguration();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
                .queryParam("owner", "user")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    @Override
    public NotionOAuthGrant exchangeAuthorizationCode(String code) {
        JsonNode response = postOAuthToken(Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri
        ));
        return new NotionOAuthGrant(
                requiredText(response, "access_token"),
                requiredText(response, "refresh_token"),
                text(response, "bot_id"),
                requiredText(response, "workspace_id"),
                text(response, "workspace_name"),
                response.path("owner").path("user").path("name").asText(null),
                response.path("owner").path("user").path("person").path("email").asText(null)
        );
    }

    @Override
    public NotionRefreshedTokens refreshAccessToken(String refreshToken) {
        JsonNode response = postOAuthToken(Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken
        ));
        return new NotionRefreshedTokens(
                requiredText(response, "access_token"),
                requiredText(response, "refresh_token")
        );
    }

    @Override
    public NotionProviderPages searchPages(
            String accessToken, String query, String cursor, int size
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("filter", Map.of("property", "object", "value", "page"));
        body.put("page_size", size);
        if (StringUtils.hasText(query)) {
            body.put("query", query.trim());
        }
        if (StringUtils.hasText(cursor)) {
            body.put("start_cursor", cursor);
        }

        JsonNode response = execute(() -> client(accessToken).post()
                .uri("/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));

        List<NotionProviderPage> pages = new ArrayList<>();
        for (JsonNode item : response.path("results")) {
            pages.add(toPage(item));
        }
        return new NotionProviderPages(
                pages,
                text(response, "next_cursor"),
                response.path("has_more").asBoolean(false)
        );
    }

    @Override
    public NotionProviderPage retrievePage(String accessToken, String pageId) {
        JsonNode response = execute(() -> client(accessToken).get()
                .uri("/v1/pages/{pageId}", pageId)
                .retrieve()
                .body(JsonNode.class));
        return toPage(response);
    }

    @Override
    public void revokeAccessToken(String accessToken) {
        requireOAuthConfiguration();
        execute(() -> restClientBuilder.baseUrl(apiBaseUrl).build().post()
                .uri("/v1/oauth/revoke")
                .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", accessToken))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public seungyong.helpmebackend.portfolio.application.port.out.NotionPageWriteResult write(
            String accessToken,
            String parentPageId,
            seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument document,
            boolean checkTitleConflict,
            seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction conflictAction,
            String conflictPageId
    ) {
        if (conflictAction == seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction.UPDATE) {
            if (!StringUtils.hasText(conflictPageId)) {
                throw new NotionProviderException(NotionProviderException.Failure.BAD_REQUEST);
            }
            appendUpdatedSnapshot(accessToken, conflictPageId, document);
            JsonNode page = retrievePageNode(accessToken, conflictPageId);
            return new seungyong.helpmebackend.portfolio.application.port.out.NotionPageWriteResult(
                    false, conflictPageId, document.title(), text(page, "url"));
        }

        if (conflictAction == null && checkTitleConflict) {
            JsonNode existing = findExactChild(accessToken, parentPageId, document.title());
            if (existing != null) {
                return new seungyong.helpmebackend.portfolio.application.port.out.NotionPageWriteResult(
                        true, requiredText(existing, "id"), document.title(), text(existing, "url"));
            }
        }

        String title = conflictAction == seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction.COPY
                ? document.title() + " (복사본)" : document.title();
        JsonNode created = createPage(accessToken, parentPageId, title, document);
        return new seungyong.helpmebackend.portfolio.application.port.out.NotionPageWriteResult(
                false, requiredText(created, "id"), title, requiredText(created, "url"));
    }

    private JsonNode findExactChild(String accessToken, String parentPageId, String title) {
        JsonNode response = execute(() -> client(accessToken).post().uri("/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", title, "filter", Map.of("property", "object", "value", "page"),
                        "page_size", 20))
                .retrieve().body(JsonNode.class));
        for (JsonNode page : response.path("results")) {
            if (parentPageId.equals(page.path("parent").path("page_id").asText())
                    && title.equals(pageTitle(page))) return page;
        }
        return null;
    }

    private JsonNode createPage(String accessToken, String parentPageId, String title,
                                seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument document) {
        return execute(() -> client(accessToken).post().uri("/v1/pages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("parent", Map.of("page_id", parentPageId),
                        "properties", titleProperty(title), "children", blocks(document, false)))
                .retrieve().body(JsonNode.class));
    }

    private void appendUpdatedSnapshot(
            String accessToken,
            String pageId,
            seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument document
    ) {
        // 사용자 작성 블록을 삭제하지 않고 동일 페이지에 새 snapshot 구역을 추가하는 안전한 update
        execute(() -> client(accessToken).patch().uri("/v1/blocks/{pageId}/children", pageId)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("children", blocks(document, true)))
                .retrieve().toBodilessEntity());
    }

    private JsonNode retrievePageNode(String accessToken, String pageId) {
        return execute(() -> client(accessToken).get().uri("/v1/pages/{pageId}", pageId)
                .retrieve().body(JsonNode.class));
    }

    private Map<String, Object> titleProperty(String title) {
        return Map.of("title", Map.of("title", List.of(Map.of("type", "text",
                "text", Map.of("content", title)))));
    }

    private List<Map<String, Object>> blocks(
            seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument document,
            boolean updated
    ) {
        List<Map<String, Object>> values = new ArrayList<>();
        if (updated) values.add(block("업데이트된 포트폴리오", "heading_1"));
        values.add(block(document.periodStart() + " - " + document.periodEnd(), "paragraph"));
        for (seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument.Section section
                : document.content().sections()) {
            values.add(block(section.title(), "heading_2"));
            for (String part : splitRichText(section.contentMd())) values.add(block(part, "paragraph"));
        }
        if (!document.evidenceLinks().isEmpty()) {
            values.add(block("근거 링크", "heading_2"));
            for (seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument.EvidenceLink link
                    : document.evidenceLinks()) {
                values.add(block(link.label() + ": " + link.url(), "bulleted_list_item"));
            }
        }
        if (values.size() > 100) throw new NotionProviderException(NotionProviderException.Failure.BAD_REQUEST);
        return values;
    }

    private Map<String, Object> block(String content, String type) {
        return Map.of("object", "block", "type", type, type,
                Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", content)))));
    }

    private List<String> splitRichText(String content) {
        if (content == null || content.isBlank()) return List.of("");
        List<String> values = new ArrayList<>();
        for (int start = 0; start < content.length(); start += 1900) {
            values.add(content.substring(start, Math.min(start + 1900, content.length())));
        }
        return values;
    }

    private String pageTitle(JsonNode page) {
        for (Map.Entry<String, JsonNode> field : page.path("properties").properties()) {
            JsonNode property = field.getValue();
            if ("title".equals(property.path("type").asText())) {
                StringBuilder title = new StringBuilder();
                for (JsonNode text : property.path("title")) title.append(text.path("plain_text").asText(""));
                return title.toString();
            }
        }
        return "";
    }

    private JsonNode postOAuthToken(Map<String, Object> body) {
        requireOAuthConfiguration();
        return execute(() -> restClientBuilder.baseUrl(apiBaseUrl).build().post()
                .uri("/v1/oauth/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));
    }

    private RestClient client(String accessToken) {
        return restClientBuilder.baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader("Notion-Version", apiVersion)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private <T> T execute(Request<T> request) {
        try {
            T response = request.execute();
            if (response == null) {
                throw new NotionProviderException(NotionProviderException.Failure.UPSTREAM);
            }
            return response;
        } catch (RestClientResponseException e) {
            throw map(e.getStatusCode(), e.getResponseHeaders());
        } catch (RestClientException e) {
            throw new NotionProviderException(NotionProviderException.Failure.UPSTREAM);
        }
    }

    private NotionProviderException map(HttpStatusCode status, HttpHeaders headers) {
        if (status.value() == 401) return new NotionProviderException(NotionProviderException.Failure.UNAUTHORIZED);
        if (status.value() == 403) return new NotionProviderException(NotionProviderException.Failure.FORBIDDEN);
        if (status.value() == 404) return new NotionProviderException(NotionProviderException.Failure.NOT_FOUND);
        if (status.value() == 400) return new NotionProviderException(NotionProviderException.Failure.BAD_REQUEST);
        if (status.value() == 429) {
            long retryAfter = 1;
            if (headers != null) {
                try {
                    retryAfter = Long.parseLong(headers.getFirst(HttpHeaders.RETRY_AFTER));
                } catch (RuntimeException ignored) {
                    // Notion이 Retry-After를 누락하거나 잘못 반환하면 최소 1초를 사용
                }
            }
            return new NotionProviderException(NotionProviderException.Failure.RATE_LIMIT, retryAfter);
        }
        return new NotionProviderException(NotionProviderException.Failure.UPSTREAM);
    }

    private NotionProviderPage toPage(JsonNode node) {
        // Notion 페이지에 제목이 없거나 제목 내용이 비어 있을 때 사용할 기본 값
        String title = "제목 없음";

        // Notion 페이지의 properties에는 제목, 날짜, 상태 등 사용자가 만든 여러 속성이 있음.
        JsonNode properties = node.path("properties");
        for (Map.Entry<String, JsonNode> field : properties.properties()) {
            JsonNode property = field.getValue();

            // title 객체 안 type 필드가 "title"이면 제목 속성
            if ("title".equals(property.path("type").asText())) {
                // 제목은 여러 개로 나뉠 수 있으므로 plain_text를 순서대로 합침
                StringBuilder value = new StringBuilder();
                for (JsonNode text : property.path("title")) {
                    value.append(text.path("plain_text").asText(""));
                }

                // 빈 제목이면 위에서 정한 "제목 없음"을 유지
                if (!value.isEmpty()) title = value.toString();
                break;
            }
        }

        // last_edited_time은 응답에 없거나 null일 수 있으므로 값이 있을 때만 파싱
        String lastEditedTime = text(node, "last_edited_time");
        OffsetDateTime editedAt = lastEditedTime == null || lastEditedTime.isBlank()
                ? null
                : OffsetDateTime.parse(lastEditedTime);

        // 페이지 ID는 후속 조회와 기본 상위 페이지 저장에 필수이므로 누락 시 provider 오류로 처리
        return new NotionProviderPage(requiredText(node, "id"), title, editedAt);
    }

    private String basicAuthorization() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void requireOAuthConfiguration() {
        if (!StringUtils.hasText(clientId)
                || !StringUtils.hasText(clientSecret)
                || !StringUtils.hasText(redirectUri)) {
            throw new NotionProviderException(NotionProviderException.Failure.UPSTREAM);
        }
    }

    private String requiredText(JsonNode node, String field) {
        // 필수 문자열은 null, 빈 문자열, 공백 문자열을 모두 허용하지 않음
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            throw new NotionProviderException(NotionProviderException.Failure.UPSTREAM);
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        // 선택 필드는 JSON에 없거나 명시적 null이면 Java null로 통일
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    @FunctionalInterface
    private interface Request<T> {
        T execute();
    }
}
