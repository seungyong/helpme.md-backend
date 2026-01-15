package seungyong.helpmebackend.usecase.port.out.github;

import seungyong.helpmebackend.domain.entity.user.GithubUser;

public interface GithubPortOut {
    String generateLoginUrl(String state);
    String getAccessToken(String code);
    GithubUser getGithubUser(String accessToken);
}
