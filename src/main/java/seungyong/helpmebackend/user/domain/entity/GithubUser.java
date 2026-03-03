package seungyong.helpmebackend.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;

@Getter
@AllArgsConstructor
public class GithubUser {
    private String name;
    private Long githubId;
    private String githubToken;

    /**
     * 깃허브 토큰을 업데이트하는 메서드입니다.
     * <br /> <br />
     * - 반드시 EncryptedToken 객체(암호화 토큰)를 인자로 받아야 하며, 내부적으로 토큰 값을 추출하여 저장합니다.
     *
     * @param newToken 업데이트할 새로운 암호화된 토큰
     */
    public void updateGithubToken(EncryptedToken newToken) {
        this.githubToken = newToken.value();
    }
}
