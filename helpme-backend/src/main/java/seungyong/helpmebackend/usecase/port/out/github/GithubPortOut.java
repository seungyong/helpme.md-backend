package seungyong.helpmebackend.usecase.port.out.github;

import seungyong.helpmebackend.domain.entity.user.GithubUser;

public interface GithubPortOut {
    String getAccessToken(String code);
    GithubUser getGithubUser(String accessToken);
}
