package seungyong.helpmebackend.domain.entity.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class User {
    private Long id;
    private GithubUser githubUser;

    public boolean isDiffToken(String newToken) {
        return !this.githubUser.getGithubToken().equals(newToken);
    }

    public void updateGithubToken(String newToken) {
        this.githubUser.setGithubToken(newToken);
    }
}
