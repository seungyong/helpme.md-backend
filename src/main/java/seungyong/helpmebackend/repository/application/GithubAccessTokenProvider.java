package seungyong.helpmebackend.repository.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

@Component
@RequiredArgsConstructor
class GithubAccessTokenProvider {
    private final UserPortOut userPortOut;
    private final CipherPortOut cipherPortOut;

    public String get(Long userId) {
        User user = userPortOut.getById(userId);
        return cipherPortOut.decrypt(user.getGithubUser().getGithubToken().value());
    }
}
