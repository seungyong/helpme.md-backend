package seungyong.helpmebackend.usecase.port.out.github.oauth2;

import seungyong.helpmebackend.adapter.out.result.OAuthTokenResult;
import seungyong.helpmebackend.domain.entity.installation.Installation;
import seungyong.helpmebackend.domain.entity.user.GithubUser;

import java.util.ArrayList;

public interface OAuth2PortOut {
    String generateLoginUrl(String state);
    OAuthTokenResult getAccessToken(String code);
    GithubUser getGithubUser(String accessToken);
    ArrayList<Installation> getInstallations(String accessToken);
}
