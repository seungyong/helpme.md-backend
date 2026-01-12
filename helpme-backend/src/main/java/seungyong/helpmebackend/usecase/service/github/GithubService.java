package seungyong.helpmebackend.usecase.service.github;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.adapter.out.persistence.mapper.UserPortOutMapper;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.usecase.port.in.github.GithubPortIn;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortOut;
import seungyong.helpmebackend.usecase.port.out.jwt.JWTPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

@Service
@RequiredArgsConstructor
public class GithubService implements GithubPortIn {
    private final JWTPortOut jwtPortOut;
    private final GithubPortOut githubPortOut;
    private final UserPortOut userPortOut;

    @Override
    public JWT signupOrLogin(String code) {
        String accessToken = githubPortOut.getAccessToken(code);
        Long githubId = githubPortOut.getGithubId(accessToken);

        User user = userPortOut.getByGithubId(githubId)
                .orElseGet(() -> userPortOut.save(UserPortOutMapper.INSTANCE.toDomainEntity(githubId, accessToken)));

        return jwtPortOut.generate(user.getId());
    }
}
