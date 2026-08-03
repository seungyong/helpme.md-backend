package seungyong.helpmebackend.auth.application.port.out;

import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;

public interface OAuth2PortOut {
    String generateLoginUrl(String state);
    OAuthTokenResult getAccessToken(String code);
    OAuthGithubUser getGithubUser(String accessToken);
}
