package seungyong.helpmebackend.auth.application.port.out;

import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.auth.domain.entity.Installation;
import seungyong.helpmebackend.user.domain.entity.GithubUser;

import java.util.List;

public interface OAuth2PortOut {
    String generateLoginUrl(String state);
    OAuthTokenResult getAccessToken(String code);
    GithubUser getGithubUser(String accessToken);
    List<Installation> getInstallations(String accessToken);
}
