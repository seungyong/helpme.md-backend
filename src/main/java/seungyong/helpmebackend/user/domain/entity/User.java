package seungyong.helpmebackend.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;

@Getter
@AllArgsConstructor
public class User {
    private Long id;
    private GithubUser githubUser;

    /**
     * 유저의 깃허브 토큰이 새로운 토큰과 다른지 확인하는 메서드
     *
     * @param newToken 비교할 새로운 토큰
     * @return 현재 저장된 토큰과 새로운 토큰이 다르면 true, 같으면 false
     */
    public boolean isDiffToken(String newToken) {
        return !this.githubUser.getGithubToken().equals(newToken);
    }

    /**
     * 유저의 깃허브 토큰을 새로운 토큰으로 업데이트하는 메서드
     * <br /> <br />
     * - 반드시 EncryptedToken 객체(암호화 토큰)를 인자로 받아야 하며, 내부적으로 토큰 값을 추출하여 저장합니다.
     *
     * @param newToken 업데이트할 새로운 암호화된 토큰
     */
    public void updateGithubToken(EncryptedToken newToken) {
        this.githubUser.updateGithubToken(newToken);
    }
}
