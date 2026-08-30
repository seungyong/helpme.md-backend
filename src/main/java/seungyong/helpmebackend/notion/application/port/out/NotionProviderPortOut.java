package seungyong.helpmebackend.notion.application.port.out;

import seungyong.helpmebackend.notion.application.port.out.result.NotionOAuthGrant;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPage;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPages;
import seungyong.helpmebackend.notion.application.port.out.result.NotionRefreshedTokens;

public interface NotionProviderPortOut {
    String buildAuthorizationUrl(String state);

    NotionOAuthGrant exchangeAuthorizationCode(String code);

    NotionRefreshedTokens refreshAccessToken(String refreshToken);

    NotionProviderPages searchPages(
            String accessToken, String query, String cursor, int size
    );

    NotionProviderPage retrievePage(String accessToken, String pageId);

    void revokeAccessToken(String accessToken);
}
