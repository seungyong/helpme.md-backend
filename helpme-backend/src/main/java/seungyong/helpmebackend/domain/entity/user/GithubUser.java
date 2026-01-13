package seungyong.helpmebackend.domain.entity.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GithubUser {
    private String name;
    private Long githubId;
    private String githubToken;
}
