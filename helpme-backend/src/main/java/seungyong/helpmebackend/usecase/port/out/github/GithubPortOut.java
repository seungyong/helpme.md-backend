package seungyong.helpmebackend.usecase.port.out.github;

public interface GithubPortOut {
    String getAccessToken(String code);
    Long getGithubId(String accessToken);
}
