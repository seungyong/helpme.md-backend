package seungyong.helpmebackend.notion.application.port.out.result;

import lombok.Getter;

@Getter
public final class NotionRefreshedTokens {
    private final String accessToken;
    private final String refreshToken;

    public NotionRefreshedTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
