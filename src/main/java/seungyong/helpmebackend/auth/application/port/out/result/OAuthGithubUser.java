package seungyong.helpmebackend.auth.application.port.out.result;

public record OAuthGithubUser(
        String name,
        Long githubId
) {
}
