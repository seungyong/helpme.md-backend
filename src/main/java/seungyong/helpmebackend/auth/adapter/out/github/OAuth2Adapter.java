package seungyong.helpmebackend.auth.adapter.out.github;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.auth.application.port.out.OAuth2PortOut;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.global.config.GithubPortConfig;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GithubRateLimitException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;
import seungyong.helpmebackend.global.infrastructure.github.GithubResponseParsingException;

import java.util.Map;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class OAuth2Adapter implements OAuth2PortOut {
    private final GithubApiExecutor githubApiExecutor;
    private final GithubPortConfig githubPortConfig;

    @Override
    public String generateLoginUrl(String state) {
        return "https://github.com/login/oauth/authorize?client_id=" + githubPortConfig.getClientId()
                + "&scope=read:user"
                + "&redirect_uri=" + githubPortConfig.getRedirectUri()
                + "&state=" + state;
    }

    @Override
    public OAuthTokenResult getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        return executeOAuthRequest(() -> githubApiExecutor.executePostNoAuth(
                    url,
                    Map.of(
                            "client_id", githubPortConfig.getClientId(),
                            "client_secret", githubPortConfig.getClientSecret(),
                            "code", code,
                            "redirect_uri", githubPortConfig.getRedirectUri()
                    ),
                    OAuthTokenResult.class,
                    "exchange OAuth code"
            ));
    }

    @Override
    public OAuthGithubUser getGithubUser(String accessToken) {
        String url = "https://api.github.com/user";

        return executeOAuthRequest(() -> githubApiExecutor.executeGet(
                    url,
                    accessToken,
                    jsonNode -> new OAuthGithubUser(
                            jsonNode.get("login").asText(),
                            jsonNode.get("id").asLong()
                    ),
                    "get authenticated GitHub user"
            ));
    }

    private <T> T executeOAuthRequest(Supplier<T> request) {
        try {
            return request.get();
        } catch (GithubApiException e) {
            if (e.isRateLimited()) {
                throw new GithubRateLimitException(e.getRetryAfterSeconds());
            }
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        } catch (GithubResponseParsingException e) {
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }
}
